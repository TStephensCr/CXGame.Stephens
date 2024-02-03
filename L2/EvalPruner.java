package connectx.L2;

import connectx.CXBoard;
import connectx.CXGameState;

public class EvalPruner {
    CXBoard T;
    boolean playerA;
    int depth;


    public EvalPruner(CXBoard T, boolean playerA, int depth) {
        this.T = T.copy();
        this.playerA = playerA;
        this.depth = depth;
    }


    private int stateConverter(CXGameState state) {
        if (state == CXGameState.WINP1)
            return Integer.MAX_VALUE;
        
        if (state == CXGameState.WINP2)
            return Integer.MIN_VALUE;
         
        return 0;
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


    public int EvalalphaBetaCaller () {
        Integer[] columns = this.T.getAvailableColumns();
        int eval = playerA ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Integer c : columns) {
            this.T.markColumn(c);
            int recEval = AlphaBetaEval(this.T, !playerA, Integer.MIN_VALUE, Integer.MAX_VALUE, this.depth-1);
            
            this.T.unmarkColumn();
            
            if (playerA) {
                if (recEval > eval) {
                    eval = recEval;
                }
            }
            else {
                if (recEval < eval) {
                    eval = recEval;
                }
            }
        }
        return eval;//non mi importa quale sia la mossa giusta, a me interessa solo quale sia l'esito della partita alla profondità scelta
    }


    private int AlphaBetaEval(CXBoard T, boolean playerA, int alpha, int beta, int depth) {
        // caso base - condizioni preliminari
        if (T.gameState() == CXGameState.WINP1) 
            return Integer.MAX_VALUE;
        
        if (T.gameState() == CXGameState.WINP2) 
            return Integer.MIN_VALUE;
        
        if (T.gameState() == CXGameState.DRAW)
            return 0;
        
        if (depth == 0)
            //return staticEval(T, playerA);
            //assumo che entrambi i giocatori preferiscano continuare a giocare piuttosto che pareggiare
            if (playerA) {
                return 1;
            }
            else {
                return -1;
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
                int tempEval = AlphaBetaEval(T, false, alpha, beta, depth - 1); //calcolo il massimo in questo modo per poter aggiornare la colonna selezionata
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
                int tempEval = AlphaBetaEval(T, true, alpha, beta, depth - 1); //calcolo il minimo in questo modo per poter aggiornare la colonna selezionata
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

}