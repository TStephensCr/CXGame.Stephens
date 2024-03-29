// Version: L2
package connectx.L2;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.concurrent.TimeoutException;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class L2 implements CXPlayer{
    final int ADJWEIGHT = 0;
    private boolean first;
    
    private int TIMEOUT;
    private long START;
    private int M; // numero di righe
    private int N; // numero di colonne
    private int K; // numero di pedine da allineare per vincere
    private positionalEval evaler;
    private boolean VERBOSE = true;
    private boolean runningOutOfTime = true;
    private int ANALYSIS_MARGIN = 500; // 1 secondo di margine per l'analisi
    private int NOT_ENOUGHT_TIME = -1;
    private int MAX_DEPTH = 21; //0 for no limit
    private boolean silence = false;
    private int startingDepth = 1;
    private HashMap<Integer, Integer> cache ;
    private float timePerDepth = 0f;

	/* Default empty constructor */

    //QUANDO VADO IN EVAL HO UNA POSIZIONE NELLA QUALE DEVO ANCORA EFFETTUARE LA MIA MOSSA!!!
    //
    public L2() {}

	public void initPlayer(int M, int N, int K,  boolean first, int timeout_in_secs) {
        this.first = first;
        this.TIMEOUT = timeout_in_secs;
        this.M = M;
        this.N = N;
        this.K = K;
        this.cache = new HashMap<Integer, Integer>();
        this.timePerDepth = (float) TIMEOUT / N;
	}

    private void cacheClear(){
        this.cache.clear();
    }

	public int selectColumn(CXBoard B) {
        //IterativeDeepening(B, first);
        this.evaler = new positionalEval(B,M, N, K, first);
        if (!this.silence){
            System.out.println("last move: " + B.getLastMove());
            this.evaler.testADJ(B);
        }
        return IterativeDeepening(B, first);
	}

    private void checkTime() throws TimeoutException {
        // se il tempo è minore di 1/N del timeout totale allora lancio l'eccezione
        float timeSpent = ((float) (System.currentTimeMillis() - START) / 1000);
        // System.out.printf("! remaining time: %.2f time per move %.2f\n", timeSpent, timePerMove);
        if (timeSpent >= this.timePerDepth) 
            throw new TimeoutException();
    }

    private void checkTime(boolean fullTime) throws TimeoutException {
        if (fullTime) {
            if (System.currentTimeMillis() - START >= (TIMEOUT * 1000 - ANALYSIS_MARGIN)) { // leave a little error margin
                throw new TimeoutException();
            }
        }
        else {
            checkTime();
        }
    }

    private int depthCalculator(){
        if(M > 10 && N > 10 && K > 9)
            return 1; 
        else
            return 2;
    }
    
    public int IterativeDeepening(CXBoard B, boolean playerA) {
        START = System.currentTimeMillis();
        int depth = startingDepth;
        int move = B.getAvailableColumns()[0];
        int lastValidMove = move;

        int reccomendedDepth = depthCalculator();
        while (true) {
            try {
                checkTime();
                
                if(VERBOSE)
                    System.out.println( " -- TIME : [" + (float) (System.currentTimeMillis() - START) / 1000 + " / " + (float) TIMEOUT + "s] DEPTH : [" + depth + "]");
                
                if(depth >= reccomendedDepth)
                    runningOutOfTime = true;
                 
                move = alphaBetaCaller(B, depth, playerA);

            } catch (TimeoutException e) {
                if (VERBOSE) 
                    System.out.println("! timeout at depth: " + depth);
                
                move = lastValidMove;
                break;
            }

            depth++;
            lastValidMove = move;

            //bro wtf che senso ha sto controllo? al massimo mettilo parametrico rispetto alla dimensione della board ma comunque non è necessario
            if (MAX_DEPTH != 0 && depth > MAX_DEPTH) 
                break; // i check all the possible solution it is useless to continue

            if(depth > B.numOfFreeCells()) 
                break; // i check all the possible solution it is useless to continue
        }
        startingDepth = depth - 2 > startingDepth ? depth - 2 : startingDepth;
        runningOutOfTime = false;
        // System.out.println(toString(B.getAvailableColumns()) + " - selected [" + lastValidMove + "]");
        return lastValidMove;
    }

    public String toString(Integer[] a){
        String l = "[";
        for(Integer i : a){
            l += i + " ";
        }
        return l + "]";
    }

	public String playerName() {
		return "L2";
	}

    private int stateConverter(CXGameState state) {
        if (state == CXGameState.WINP1)
            return Integer.MAX_VALUE;
        
        if (state == CXGameState.WINP2)
            return Integer.MIN_VALUE;
         
        return 0;
    }

    private int alphaBetaCaller (CXBoard B, int depth, boolean playerA) throws TimeoutException { 
        Integer[] columns = B.getAvailableColumns();
        int eval = playerA ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int move = columns[0];

        for (Integer c : columns) {
            if (runningOutOfTime) {
                try {
                    checkTime(true);
                } catch (TimeoutException e) {
                    System.out.println("! not enought time at colunm: " + c);
                    move = NOT_ENOUGHT_TIME;
                    throw e;
                }
            }
            
            B.markColumn(c);
            int centerBias = playerA ? centerBiasCalculator(c) : -centerBiasCalculator(c); //tende a farmi giocare al centro anche alle profondità più basse :)
            int recEval = AlphaBetaForbiddenColumns(B, !playerA, Integer.MIN_VALUE, Integer.MAX_VALUE, depth-1) ;
            if (recEval < 1000 && recEval > -1000)
                recEval += centerBias;
            if (!this.silence)
                System.out.println("colonna: " + c + " eval: " + recEval);
            B.unmarkColumn();
            
            if (playerA) {
                if (recEval > eval) {
                    eval = recEval;
                    move = c;
                }
            }
            else {
                if (recEval < eval) {
                    eval = recEval;
                    move = c;
                }
            }
        }
        return move;
    }

    private int completeBoard(CXBoard B, Integer[] columns){
        int iterazioni = 0;
        int c = columns[0];
        while((!B.fullColumn(c)) && B.gameState() == CXGameState.OPEN){
            B.markColumn(c);
            iterazioni++;
        }
        CXGameState state = B.gameState();
        for (int i = 0; i < iterazioni; i++) {
            B.unmarkColumn();
        }
        int conv = stateConverter(state);
        if (conv>0) return conv-iterazioni;
        if (conv<0) return conv+iterazioni;
        return conv;
    }

    private int checkAdjacensies(CXBoard B, int x, int y, CXCellState winningPlayer){
        //controllo orizzontale, ricordo che SO di non aver vinto.
        int laterali = 0;
        for (int i = Math.max(0, x - K + 2); i < Math.min(M, x + K - 2); i++) {
            if (B.cellState(y, i) == winningPlayer)
                laterali++;
            else if (B.cellState(y, i) != CXCellState.FREE) {
                if (i < x) // vitto spiegami sto controllo // spiegato :)
                    laterali = 0;
                else 
                    break;
            }
            
        }
        //controllo verticale
        int verticali = 0;
        for (int i = 0; i < Math.min(y, K - 2); i++) {
            if (B.cellState(y - i, x) == winningPlayer) 
                verticali++;
            else if (B.cellState(y - i, x) != CXCellState.FREE)
                break;
        }
        //controllo diagonale da fare : quattro cicli che controllano le quattro mezze diagonali possibili
        /*
            * \   /   //esploro così, prima la diagonale da sinistra a destra verso il basso poi quella da destra a sinistra verso l'alto
            *  \ /
            *   O
            *  / \
            * /   \
            * 
            */

        int obliqui1 = 0;
        // up left
        int tempcounter = Math.min(Math.min(x,K-1),M-y-1);
        for (int i = 0; i < tempcounter; i++) {
            if (B.cellState(y+tempcounter-i, x-tempcounter+i) == winningPlayer)
                obliqui1++;
            else if (B.cellState(y+tempcounter-i, x-tempcounter+i) != CXCellState.FREE) 
                obliqui1 = 0;
                // credo di star controllando colonne inutili dopo aver trovato una pedina avversaria
        }
        //down right
        tempcounter = Math.min(Math.min(N-x-1,K-1),y);
        for (int i = 1; i <= tempcounter; i++) {
            if (B.cellState(y-i,x+i) == winningPlayer)
                obliqui1++;
            else if (B.cellState(y-i,x+i) != CXCellState.FREE)
                break;
        }

        int obliqui2 = 0;
        //down left
        tempcounter = Math.min(Math.min(x,K-1),y);
        for (int i = 0; i < tempcounter; i++) {
            if (B.cellState(y-tempcounter+i,x-tempcounter+i) == winningPlayer)
                obliqui2++;
            else if (B.cellState(y-tempcounter+i,x-tempcounter+i) != CXCellState.FREE)
                obliqui2 = 0;

        }
        //up right
        tempcounter = Math.min(Math.min(N-x-1,K-1),M-y-1);
        for (int i = 1; i <= tempcounter; i++) {
            if (B.cellState(y+i,x+i) == winningPlayer)
                obliqui2++;
            else if (B.cellState(y+i,x+i) != CXCellState.FREE)
                break;
        }
        return ADJWEIGHT*(laterali*laterali+verticali*verticali+obliqui1*obliqui1+obliqui2*obliqui2);
    }

    private int centerBiasCalculator(int x){
        int n = N/2;
        int centerBias  = x - n; // se è grande è lontano dal centro
        if (centerBias < 0) {
            centerBias = -centerBias;
        }
        return -centerBias * 10; // 10 è un moltiplicatore arbitrario
    }

    private int maximizerStaticEval(CXBoard b){
        CXBoard B = b.copy();
        
        int punteggio = 0;
        
        Integer[] columns = B.getAvailableColumns();
        //  ! TEMPORANEO !

        //rimanme una sola linea di gioco possibilie, la porto fino alla sua conclusione e restituisco un valore esatto.
        if (columns.length == 1)
            return this.completeBoard(B, columns);
        
        // ! FINE TEMPORANEO !
        
        ///////////////////////////////////// colonne proibite e win in 1.
        int forbiddenColumnsNumber = 0;

        for (Integer c : columns) {
            B.markColumn(c);
            CXGameState state = B.gameState();
            if (state != CXGameState.OPEN) {
                B.unmarkColumn();
                return stateConverter(state);  
            }
            if ((!B.fullColumn(c)) && B.gameState() == CXGameState.OPEN) {
                B.markColumn(c);
                if (B.gameState() == CXGameState.WINP2) {
                    
                    //c diventa una colonna proibita PER ME p2
                    forbiddenColumnsNumber++;
                }
                B.unmarkColumn();
                
            }
            B.unmarkColumn();
        } 
        punteggio -= forbiddenColumnsNumber * forbiddenColumnsNumber * 100;
        
        ///////////////////////////////////////////////// fine colonne proibite.

        //colonne obbligate MIE, posso assumere di averne libere più di una.
        List<Integer> obbligatorie = new ArrayList<Integer>();
        B.markColumn(columns[0]);
        boolean isHead = true;
        for (Integer c : columns) {
            if (isHead) {
                isHead = false;
            }
            else {
                B.markColumn(c);
                CXGameState state = B.gameState();
                if (state == CXGameState.WINP2) {
                    obbligatorie.add(c);
                }
                B.unmarkColumn();
            }
        }
        B.unmarkColumn();
        //controllo la prima che avevo precedentemente escluso a tavolino
        B.markColumn(columns[1]);
        B.markColumn(columns[0]);
        CXGameState state = B.gameState();
        if (state == CXGameState.WINP2) {
            obbligatorie.add(columns[0]);
        }
        B.unmarkColumn();
        B.unmarkColumn();
        if (obbligatorie.size() > 1) {
            return Integer.MIN_VALUE+2;
        }
        punteggio = punteggio - 1000*obbligatorie.size();

        // center bias
        CXCell lastMove = B.getLastMove();
        
        ///////////Adjacensies check (è una valutazione più sulla mossa che sulla posizione ma cionondimeno la ritengo rilevante)
        punteggio +=  this.checkAdjacensies(B, lastMove.j, lastMove.i, CXCellState.P1);

        return punteggio;
        
    }

    private int minimizerStaticEval(CXBoard b) {
        CXBoard B = b.copy();
        
        int punteggio = 0;
        int colonne_proibite = 0;
        Integer[] columns = B.getAvailableColumns();

        //TEMPORANEO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if (columns.length == 1)
            //rimanme una sola linea di gioco possibilie, la porto fino alla sua conclusione e restituisco un valore esatto.
            return this.completeBoard(B, columns);
        
        //FINE TEMPORANEO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        /////////////////////////////////////colonne proibite.
        for (Integer c : columns) {
            B.markColumn(c);
            CXGameState state = B.gameState();
            if (state != CXGameState.OPEN) {
                B.unmarkColumn();
                return stateConverter(state);  
            }
            if ((!B.fullColumn(c)) && B.gameState() == CXGameState.OPEN) {
                B.markColumn(c);
                if (B.gameState() == CXGameState.WINP1) {
                    
                    //c diventa una colonna proibita PER ME p2
                    colonne_proibite++;
                }
                B.unmarkColumn();
                
            }
            B.unmarkColumn();
        } 
        // colonne_proibite = fobiddenColumnsCalculator(B, columns, CXGameState.WINP1);
        punteggio = punteggio + (100*colonne_proibite*colonne_proibite);
        
        ///////////////////////////////////////////////fine colonne proibite.

        ///////////colonne obbligate MIE, posso assumere di averne libere più di una.
        List<Integer> obbligatorie = new ArrayList<Integer>();
        B.markColumn(columns[0]);
        Boolean isHead = true;
        for (Integer c : columns) {
            if (isHead) {
                isHead = false;
            }
            else {
                B.markColumn(c);
                CXGameState state = B.gameState();
                if (state == CXGameState.WINP1) {
                    obbligatorie.add(c);
                }
                B.unmarkColumn();
            }
        }
        B.unmarkColumn();
        ///////////controllo la prima che avevo precedentemente escluso a tavolino
        B.markColumn(columns[1]);
        B.markColumn(columns[0]);
        CXGameState state = B.gameState();
        if (state == CXGameState.WINP1) {
            obbligatorie.add(columns[0]);
        }
        B.unmarkColumn();
        B.unmarkColumn();
        if (obbligatorie.size() > 1) {
            return Integer.MAX_VALUE-2;
        }
        punteggio = punteggio + 1000*obbligatorie.size();

        //center bias
        CXCell lastMove = B.getLastMove();

        
        //controllo laterale
        punteggio -=  this.checkAdjacensies(B, lastMove.j, lastMove.i, CXCellState.P2);

        return punteggio;
    }

    //static eval basata sul numero di colonne proibite
    public int staticEval(CXBoard B,boolean playerA){
        this.evaler = new positionalEval(B,M, N, K,playerA);
        return this.evaler.eval();
    }
    //un alpha beta pruning che funziona e deve essere chiamato dalla funzione alphaBetaCaller!!!
    public int AlphaBetaForbiddenColumns(CXBoard T, boolean playerA, int alpha, int beta, int depth) {

        // caso base - condizioni preliminari
        if (T.gameState() == CXGameState.WINP1) 
            return Integer.MAX_VALUE;
        
        if (T.gameState() == CXGameState.WINP2) 
            return Integer.MIN_VALUE;
        
        if (T.gameState() == CXGameState.DRAW)
            return 0;
        
        Integer cacheValue = cache.get(this.getHash(T));
        if(cacheValue != null){
            return cacheValue.intValue();
        }
        
        if (depth == 0) {
            int finalEval = staticEval(T, playerA);
            cache.put(this.getHash(T), finalEval);
            return finalEval;
        }
    
        Integer[] columns = T.getAvailableColumns();

        //rimanme una sola linea di gioco possibilie, la porto fino alla sua conclusione e restituisco un valore esatto.
        if (columns.length == 1)
            return this.completeBoard(T, columns);
        
        // caso generico
        int finalEval;
        if (playerA) { // MAX player
            finalEval = Integer.MIN_VALUE;
            for (Integer c : columns) {
                T.markColumn(c);
                int tempEval = AlphaBetaForbiddenColumns(T, false, alpha, beta, depth - 1); //calcolo il massimo in questo modo per poter aggiornare la colonna selezionata
                if (tempEval > finalEval)                                               //quando necessario.
                    finalEval = tempEval;
                
                alpha = Math.max(finalEval, alpha);
                T.unmarkColumn();
                if (beta <= alpha) { // β cutoff
                    break;
                }
            }
            
        } else { // MIN player
            finalEval = Integer.MAX_VALUE;
            for (Integer c : columns) {
                T.markColumn(c);
                int tempEval = AlphaBetaForbiddenColumns(T, true, alpha, beta, depth - 1); //calcolo il minimo in questo modo per poter aggiornare la colonna selezionata
                if (tempEval < finalEval)                                            //quando necessario.
                    finalEval = tempEval;
                
                beta = Math.min(finalEval, beta);
                T.unmarkColumn();
                if (beta <= alpha) { // α cutoff
                    break;
                }
            }
        }

        //un return che abbassa leggermente il peso delle mosse mentre si propagano (perdere in una mossa è peggio che perdere in 2 mosse)
        if (finalEval < -1) 
            finalEval++;
        else if(finalEval > 1)
            finalEval--;
        
        return finalEval;
    }

    public int getHash(CXBoard b){
        int prime = 31;
        int result = 1;

        // Include the current state of the board
        result = prime * result + Arrays.deepHashCode(b.getBoard());

        // Include the list of marked cells
        result = prime * result + b.getMarkedCells().hashCode();

        // Include the available columns
        result = prime * result + b.getAvailableColumns().hashCode();

        // Include the current player
        result = prime * result + b.currentPlayer();

        // Include the game state
        result = prime * result + b.gameState().hashCode();

        return result;
	}
}