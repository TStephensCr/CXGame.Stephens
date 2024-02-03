package connectx.L2;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCellState;

public class positionalEval {
    CXBoard board;
    CXCellState[][] B;
    boolean playerA; //questo giocatore è quello che ha effettuato L'ULTIMA mossa
    private int M; // numero di righe
    private int N; // numero di colonne
    private int X; // numero di pedine da allineare per vincere
    private int cellWeight = 1000; 
    private int colonne_proibite = 100; // numero di colonne in cui non è possibile giocare
    private int ADJWEIGHT = 200; //peso delle pedine adiacenti


    public positionalEval(CXBoard board, int M, int N, int K, boolean playerA) {
        this.board = board;
        this.M = M; // n righe
        this.N = N; // n colonne
        this.X = K; // n pedine da allineare per vincere
        this.B = board.getBoard();
        this.playerA = playerA;
    }

    private boolean WouldBeWinningMove(int i, int j, CXCellState s) { 
        int n;
        n = 1;
        for (int k = 1; j-k >= 0 && B[i][j-k] == s; k++) n++; // backward check
        for (int k = 1; j+k <  N && B[i][j+k] == s; k++) n++; // forward check
        if (n >= X) return true;

        // Vertical check
        n = 1;
        for (int k = 1; i+k <  M && B[i+k][j] == s; k++) n++;
        if (n >= X) return true;

        // Diagonal check
        n = 1;
        for (int k = 1; i-k >= 0 && j-k >= 0 && B[i-k][j-k] == s; k++) n++; // backward check
        for (int k = 1; i+k <  M && j+k <  N && B[i+k][j+k] == s; k++) n++; // forward check
        if (n >= X) return true;

        // Anti-diagonal check
        n = 1;
        for (int k = 1; i-k >= 0 && j+k <  N && B[i-k][j+k] == s; k++) n++; // backward check
        for (int k = 1; i+k <  M && j-k >= 0 && B[i+k][j-k] == s; k++) n++; // forward check
        if (n >= X) return true;

    return false;
    }

    private int readBoard(){
        int score = 0; // per il momento modo incredibilmente rudimentale di valutare la board
        //era una funzione così bella e io l'ho sovracomplicata per ottenere un vantaggio minore dell'1%
        //eeeeeeeeeeeeeeeeeeh la tabella è scritta al contrario per qualche cazzo di motivo quindi tocca rifare todo ziocan ziocan ziocan
        int[] scoreKeeper1 = new int[this.M];
        int[] scoreKeeper2 = new int[this.M];
        for (int i = 0; i<this.M; i++) {
            scoreKeeper1[i] = 0;
            scoreKeeper2[i] = 0;
        }

        for (int l = 0; l<this.N; l++) {
            boolean SeaLevelSet = false;//non importa veramente "l'altezza assoluta della mossa" quanto più l'altezza relativa all'ultima casella occupata nella colonna.
            int SeaLevel = 0;
            boolean wp1r = false;
            boolean wp2r = false;
            for (int k = 0; k<this.M; k++) {
                if (B[M-k-1][l] == CXCellState.FREE) {//(M-k+SeaLevel)lo scorriemto è fatto così perché la board è scritta al contrario, in questo modo la tratto come se fosse scritta normalmente
                    SeaLevelSet = true;
                    if (WouldBeWinningMove(M-k-1, l, CXCellState.P1)) {
                        //score += cellWeight + cellWeight*(M-k+SeaLevel)/M/2;
                        scoreKeeper1[k-SeaLevel]++;
                        if (k%2==0){//pari e dispari sono invertiti perchè K è indicizzato a zero (concetto di "chi ci finirà")
                            score += cellWeight + cellWeight*(M-k+SeaLevel)/M/2;
                        }
                        
                        //questo bego incredibile si accorge se ci sono due mosse vincenti consecutive
                        if (wp1r == true) {
                            score += 170000*(M-k+SeaLevel);
                            wp1r = false;
                        }
                        else {
                            wp1r = true;
                        }
                    }
                    else{
                        wp1r = false;
                    }
                    if (WouldBeWinningMove(M-k-1, l, CXCellState.P2)) {
                        //score -= cellWeight + cellWeight*(M-k+SeaLevel)/M/2;
                        scoreKeeper2[k-SeaLevel]++;
                        if (k%2==1){//pari e dispari sono invertiti perchè K è indicizzato a zero
                            score += cellWeight + cellWeight*(M-k+SeaLevel)/M/2;
                        }

                        //questo bego incredibile si accorge se ci sono due mosse vincenti consecutive
                        if (wp2r == true) {
                            score -= 170000*(M-k+SeaLevel);
                            wp2r = false;
                        }
                        else {
                            wp2r = true;
                        }
                    }
                    else{
                        wp2r = false;
                    }
                }
                if(!SeaLevelSet) SeaLevel++;
            }
        }
        //NON MI PONGO IL PROBLEMA DI BOARD DI DIMENSIONE INFERIORE A 2x2 PERCHE' IL GIOCO NON HA SENSO
        //trattamento dei dati negli scoreKeeper
        int forbiddenColumns1 = 0;
        int forbiddenColumns2 = 0;
        //due mosse vincenti simultanee a livello del mare zero sono letali
        if (scoreKeeper1[0] >1) {
            return Integer.MAX_VALUE;
        }
        else {
            score += cellWeight*scoreKeeper1[0];
        }
        if (scoreKeeper2[0] >1) {
            return Integer.MIN_VALUE;
        }
        else {
            score -= cellWeight*scoreKeeper2[0];
        }

        forbiddenColumns1 = scoreKeeper1[1];//sono quelle a favore di 1
        score = score + (colonne_proibite*forbiddenColumns1*forbiddenColumns1);
        forbiddenColumns2 = scoreKeeper2[1];//sono quelle a favore di 2
        score = score - (colonne_proibite*forbiddenColumns2*forbiddenColumns2);

        for (int i = 2; i<this.M; i++) {
            score += scoreKeeper1[i]*(cellWeight + cellWeight*(M-i)/M/2);
            score -= scoreKeeper2[i]*(cellWeight + cellWeight*(M-i)/M/2);
        }

        return score;
    }

    private int supermassiveADJ(CXBoard B,CXCellState winningPlayer,boolean test){
        int tot = 0;
        for (int y = 0; y<this.M; y++) {
            for (int x = 0; x<this.N; x++) {
                if(B.cellState(y, x)==winningPlayer) {
                    int possible = 0; //questo intero conta gli spazi vuoti e le celle corrette, se non sono almeno K non ha senso controllare
                    //controllo orizzontale, ricordo che SO di non aver vinto.
                    int laterali = 0;
                    for (int i = Math.max(0, x - this.X + 1); i < Math.min(N, x + this.X); i++) {
                        
                        if(i != x) { // non conto la cella in cui ho appena messo la pedina
                            if (B.cellState(y, i) == winningPlayer) {
                                laterali++;
                                possible++;
                            }  
                            else if (B.cellState(y, i) != CXCellState.FREE) {
                                if (i < x) {// vitto spiegami sto controllo // spiegato :)
                                    laterali = 0;
                                    possible = 0;
                                }
                                else{
                                    if(test)
                                        System.out.println("break");
                                    break;
                                }
                            }
                            else {
                                possible++;
                            }
                        }
                    }
                    if (possible<this.X-1)
                        laterali = 0;
                    //controllo verticale
                    int verticali = 0;
                    possible = 0;
                    for (int i = Math.max(0,y-this.X+1); i < this.M; i++) {
                        if (i != y ) {// non conto la cella in cui ho appena messo la pedina e le celle sono numerate al contrario
                            if(B.cellState(i,x) == winningPlayer) {
                                verticali++;
                                possible++;
                            }
                            else if (B.cellState(i,x) != CXCellState.FREE) {
                                if (i < y) {
                                    verticali = 0;
                                    possible = 0;
                                    break;
                                }
                                else 
                                    break;
                            }
                            else {
                                possible++;
                            }
                        }
                    }
                    if (possible<this.X-1)
                        verticali = 0;

                    int obliqui1 = 0;
                    possible = 0;
                    // up left
                    int tempcounter = Math.min(Math.min(x,this.X-1),M-y-1);
                    for (int i = 0; i < tempcounter; i++) {
                        if (B.cellState(y+tempcounter-i, x-tempcounter+i) == winningPlayer) {
                            obliqui1++;
                            possible ++;
                        }
                        else {
                            if (B.cellState(y+tempcounter-i, x-tempcounter+i) != CXCellState.FREE) {
                                obliqui1 = 0;
                                possible =0;
                            }
                            else {
                                possible++;
                            }
                        }
                    }
                    //down right
                    tempcounter = Math.min(Math.min(N-x-1,this.X-1),y);
                    for (int i = 1; i <= tempcounter; i++) {
                        if (B.cellState(y-i,x+i) == winningPlayer) {
                            obliqui1++;
                            possible++;
                        } 
                        else if (B.cellState(y-i,x+i) != CXCellState.FREE)
                                break;
                            else {
                                possible++;
                            }
                    }
                    if (possible < this.X-1)
                        obliqui1 = 0;

                    int obliqui2 = 0;
                    possible = 0;
                    //down left
                    tempcounter = Math.min(Math.min(x,this.X-1),y);
                    for (int i = 0; i < tempcounter; i++) {
                        if (B.cellState(y-tempcounter+i,x-tempcounter+i) == winningPlayer) {
                            obliqui2++;
                            possible++;
                        }
                        else {
                            if (B.cellState(y-tempcounter+i,x-tempcounter+i) != CXCellState.FREE){
                                obliqui2 = 0;
                                possible = 0;
                            }
                            else{
                                possible++;
                            }
                        }
                    }
                    //up right
                    tempcounter = Math.min(Math.min(N-x-1,this.X-1),M-y-1);
                    for (int i = 1; i <= tempcounter; i++) {
                        if (B.cellState(y+i,x+i) == winningPlayer){
                            obliqui2++;
                            possible++;
                        }
                        else if (B.cellState(y+i,x+i) != CXCellState.FREE)
                                break;
                            else{
                                possible++;
                            }
                    }
                    if (possible < this.X-1)
                        obliqui2 = 0;

                    if (test)
                        System.out.println("posizione "+ y+" "+x +" laterali: " + laterali + " verticali: " + verticali + " obliqui1: " + obliqui1 + " obliqui2: " + obliqui2);
                    
                    tot = tot + ADJWEIGHT*(laterali+verticali+obliqui1+obliqui2);
                }
            }
        }
        
        return tot;
    }

    private int ADJcheck(CXBoard B) {
        return supermassiveADJ(B, CXCellState.P1,false) - supermassiveADJ(B, CXCellState.P2,false);
    }

    public void testADJ(CXBoard B){
        System.out.println("ADJcheck: " + supermassiveADJ(B, CXCellState.P1,true) + " "  + supermassiveADJ(B, CXCellState.P2,true));
    }

    public int eval (){
        //controllo se la partita termina in modo certo a profondità 2
        EvalPruner pruner = new EvalPruner(this.board, this.playerA, 2);//quel 2 magico serve per avere un costo computazionale simile a quello di readBoard()
        //eval pruner fa complete board quando necessario
        int eval = pruner.EvalalphaBetaCaller();
        if (eval !=1 && eval != -1) {
            return eval;
        }
        eval = 0;
        eval = eval + readBoard();
        eval = eval + ADJcheck(this.board);
        return eval ;
    }
}