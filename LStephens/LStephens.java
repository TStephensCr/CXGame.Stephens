package connectx.LStephens;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class LStephens implements CXPlayer {
	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int  TIMEOUT;
	private long START;
	private int depth;
	private boolean first;
	private int height, width;
	private boolean blackList[] = new boolean[100];
	private int depthIndex;
	private static int maxDepth[] = {5, 2, 0};
	//private static int resultMultiplier = 10;
	//private static int winMultiplier = 20;
	private static int depthMultiplier = 2;

	/* Default empty constructor */
	public LStephens() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		this.first = first;
		this.height = M;
		this.width = N;
		if(width > 10)
			if(width > 20)
				this.depthIndex = 2;
			else
				this.depthIndex = 1;
		else
			this.depthIndex = 0;
		this.depth = maxDepth[depthIndex];
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	/***************** MAIN FUNCTION *********************/

	public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();
        Integer[] L = B.getAvailableColumns();
		int save = L[rand.nextInt(L.length)];
		refreshBlackList(B, L);
		for(int i : L){
			save = L[rand.nextInt(L.length)]; // Save a random column
			if(!blackList[save])
				break;
		}
        try {
            int col = singleMoveWin(B, L);
            if (col != -1){
				//System.err.println("Winning column found: "+col);
				return col;
			}

            col = singleMoveBlock(B, L);
            if (col != -1){
				//System.err.println("Blocking column found: "+col);
				return col;
			}
			checktime();
			int columnOrder[] = new int[width];
			for(int i = 0; i < width; i++){
				columnOrder[i] = width/2 + (1-2*(i%2))*(i+1)/2; 
			}

			int albe = -1;
			int tmp = 0;
			for(int x=0; x<width; x++){
				checktime();
				if(B.fullColumn(columnOrder[x]))
					continue;
				B.markColumn(columnOrder[x]);
				//System.err.println("Marking column "+columnOrder[x]+" from main");
				albe = alphaBetaSearch(B, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, first, 1);
				//System.err.println("Column "+columnOrder[x]+" has value "+albe);
				if(first){
					if(albe > tmp){
						tmp = albe;
						col = columnOrder[x];
					}
				}
				else{
					if(albe < tmp){
						tmp = albe;
						col = columnOrder[x];
					}
				}
				B.unmarkColumn();
			}
			//System.err.println("Selected column "+col+" with value "+tmp);
            if (col != -1) {
				if(B.fullColumn(col)) {
					//System.err.println("Column "+col+" is full");
					return save;
				}
				else if(!blackList[col])
					return col;
				else {
					//System.err.println("Column "+col+" is blacklisted");
					return save;
				}
			} 
			else {
				//System.err.println("No column found");
				return save;
			}
        } catch (TimeoutException e) {
			System.err.println("Timeout in selectColumn");
            return save;
        }
    }

	/***************** HELPER FUNCTIONS *********************/

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);

            if (state == myWin){
				B.unmarkColumn();
                return i; // Winning column found: return immediately
			}

            B.unmarkColumn();
        }
        return -1;
    }

    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        int tmp = -1;
		boolean found = false;
		int randomVal = L[rand.nextInt(L.length)];
		int randomVal2 = randomVal;
		Integer[] L2 = B.getAvailableColumns();
		B.markColumn(randomVal); 
		for (int i : L) {
			if(B.fullColumn(i) || i == randomVal)
				continue;
			CXGameState state = B.markColumn(i);
			if(state == yourWin) {
				tmp = i;
				B.unmarkColumn();
				found = true;
				break;
			}
			B.unmarkColumn();
        }
		B.unmarkColumn();
		if(!found){
			if(L2.length == 1)
				return tmp;
			do{
				randomVal2 = L[rand.nextInt(L.length)];
			}while(randomVal2 == randomVal && !B.fullColumn(randomVal2));
			B.markColumn(randomVal2);
			CXGameState state = B.markColumn(randomVal);
			if(state == yourWin) {
				tmp = randomVal;
			}
			B.unmarkColumn();
			B.unmarkColumn();
		}

        return tmp;
    }

	private void refreshBlackList(CXBoard B, Integer[] L) {
		if(L.length <= 1 || L == null)
			return;
		for(int i : L){
			if(B.fullColumn(i))
				continue;
			if(B.markColumn(i) == myWin){
				B.unmarkColumn();
				continue;
			}
			if(B.fullColumn(i)){
				B.unmarkColumn();
				continue;
			}
			if(B.markColumn(i) == yourWin){
				this.blackList[i] = true;	
			}else this.blackList[i] = false;
			B.unmarkColumn();
			B.unmarkColumn();
		}
	}

	/***************** ALPHA-BETA SEARCH *********************/

    private int alphaBetaSearch(CXBoard B, int depth, int alpha, int beta, boolean first, int curDepth){
	try{
		checktime();
		//System.err.println("Entering alphaBeta");
		// Final depth reached or game ended
		CXGameState gameState = B.gameState();
		if (depth == 0 || !gameState.equals(CXGameState.OPEN)) {
			int eval = evaluateBoard(B,curDepth);
			//System.err.println("Eval :"+eval);
			if (first) {
				return (eval > alpha) ? eval : alpha;
			} else {
				return (eval < beta) ? eval : beta;
			}
		}else{
			//explore central moves first
			int columnOrder[] = new int[width];
			for(int i = 0; i < width; i++){
				columnOrder[i] = width/2 + (1-2*(i%2))*(i+1)/2; 
			}

			if(first){
				int maxEval = Integer.MIN_VALUE;
				//System.err.println("Entering first");
				for(int x = 0; x < width; x++){
					if(B.fullColumn(columnOrder[x]))
						continue;
					B.markColumn(columnOrder[x]);
					//System.err.println("Marked column "+columnOrder[x]);
					int eval = alphaBetaSearch(B, depth - 1, alpha, beta, false, curDepth + 1);
					B.unmarkColumn();
					//System.err.println("Unmarked column "+columnOrder[x]);
					maxEval = Math.max(maxEval, eval);
					alpha = Math.max(alpha, eval);
					if(beta <= alpha)
						break;
				}
				//System.err.println("Best value with curDepth at "+curDepth+" is "+maxEval);
				return maxEval;
			}else{
				int minEval = Integer.MAX_VALUE;
				//System.err.println("Entering second");
				for(int x = 0; x < width; x++){
					if(B.fullColumn(columnOrder[x]))
						continue;
					B.markColumn(columnOrder[x]);
					//System.err.println("Marked column "+columnOrder[x]);
					int eval = alphaBetaSearch(B, depth - 1, alpha, beta, true, curDepth + 1);
					B.unmarkColumn();
					//System.err.println("Unmarked column "+columnOrder[x]);
					minEval = Math.min(minEval, eval);
					beta = Math.min(beta, eval);
					if(beta <= alpha)
						break;
				}
				//System.err.println("Best value with curDepth at "+curDepth+" is "+minEval);
				return minEval;
			} 
		}
	}catch(TimeoutException e){
		System.err.println("Timeout in alphaBetaSearch");
		if (first) {
			return alpha;
		} else {
			return beta;
		}
	}
	}

	/***************** EVALUATION FUNCTIONS *********************/

	public int evaluateBoard(CXBoard B, int curDepth) throws TimeoutException{
		try{
			checktime();
			//int player1Score = calculatePlayerScore(B, CXCellState.P1);
			//int player2Score = calculatePlayerScore(B, CXCellState.P2);
			//System.err.println("Player 1 score: "+player1Score);
			//System.err.println("Player 2 score: "+player2Score);

			/*int depthBonus = (maxDepth[depthIndex] - curDepth)*depthMultiplier;
			player1Score += depthBonus;
			player2Score += depthBonus;*/

			//return player1Score - player2Score;

			return evaluateHeuristic(B);

		}catch(TimeoutException e){
			System.err.println("Timeout in evaluateBoard");
			return 0;
		}
    }

	private int calculatePlayerScore(CXBoard B, CXCellState player) throws TimeoutException{
		int playerScore = 0;
		CXCell[] MC = B.getMarkedCells();
		int centerColumn = width / 2;
		try{
			checktime();
			playerScore += horizontalScore(B, player);
			
			playerScore += verticalScore(B, player);
			
			playerScore += diagonalManager(B, player);

			return playerScore;
		}catch(TimeoutException e){
			System.err.println("Timeout in calculatePlayerScore");
			return 0;
		}
	}
    
	private int horizontalScore(CXBoard B, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		for(int i=0;i<height;i++){
			length = 0;
			for(int j=0;j<width;j++){
				if(C[i][j] == player)
					length++;
				else{
					if(C[i][j] != CXCellState.FREE)
						if(length > 2)
							length--;
					//if(length > 1)
						//result += length;
					if(length >= B.X)
						return 1000000;
					length = 0;
				}
			}
			if(length > 1)
				result += length;
			if(length >= B.X)
				return 1000000;
		}
		return result*(result-1);
	}

	private int verticalScore(CXBoard B, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		for(int i=0;i<width;i++){
			length = 0;
			for(int j=height-1;j>=0;j--){
				if(C[j][i] == player)
					length++;
				else{
					if(C[j][i] != CXCellState.FREE)
						if(length >= 2)
							length--;
					if(length > 1)
						result += length;
					if(length >= B.X)
						return 1000000;
					length = 0;
				}
			}
			if(length > 1)
				result += length;
			if(length >= B.X)
				return 1000000;
		}
		return result*(result-1);
	}

	private int diagonalManager(CXBoard B, CXCellState player) {//Serve un disegno per capire sta funzione mi sa
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		//diagonali verso il centro che iniziano dalle colonne di lato, ignorando le diagonali troppo corte per fare X in fila
		for(int i=B.X-1; i<height-1; i++){
			result += diagonalUpLeft(B, i, 0, player);

			result += diagonalUpRight(B, i, width-1, player);
			
			result += diagonalUpLeft(B, height-1, width-1, player);
			
		}

		//diagonali verso il centro che iniziano dalla riga più in alto, ignorando le diagonali troppo corte per fare X in fila
		for(int i=1; i<(width-B.X+1); i++){
			result += diagonalUpLeft(B, height-1, i, player);
			
			result += diagonalUpRight(B, height-1, (width-1), player);
			
		}
		return result;
	}

	private int diagonalUpLeft(CXBoard B, int row, int col, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		int i = row;
		int j = col;
		while(isValidPosition(B, i, j)){
			if(C[i][j] == player)
				length++;
			else{
				if(C[i][j] != CXCellState.FREE)
					if(length > 2)
						length--;
				if(length > 1)
					result += length;
				if(length >= B.X)
					return 1000000;
				length = 0;
			}
			i--;
			j++;
		}
		if(length > 1)
			result += length;
		if(length >= B.X)
			return 1000000;
		
		return result*(result-1);
	}

	private int diagonalUpRight(CXBoard B, int row, int col, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		int i = row;
		int j = col;
		while(isValidPosition(B, i, j)){
			if(C[i][j] == player)
				length++;
			else{
				if(C[i][j] != CXCellState.FREE)
					if(length > 2)
						length--;
				if(length > 1)
					result += length;
				if(length >= B.X)
					return 1000000;
				length = 0;
			}
			i--;
			j--;
		}
		if(length > 1)
			result += length;
		if(length >= B.X)
			return 1000000;
		
		return result*(result-1);
	}



	private int evaluateHeuristic(CXBoard localB) {
		CXCellState[][] C = localB.getBoard();

		int evalRow = 0;
		int evalCol = 0;
		int evalDiag = 0;

		// Valutazione righe  (suppongo M=7,N=7,X=5)
		for (int i = 0; i < localB.M; i++) {                  // per ogni riga  (7 righe)
		for (int j = 0; j <= localB.N - localB.X; j++) {      // per ogni colonna da 0 a N-X    (0 - 7-5=2)
			int countPlayer = 0;    // celle del giocatore  
			int countOpponent = 0;  // celle dell'avversario
			int countFree = 0;      // celle libere

			/* analizza tutte le possibili sequenze di 5 gettoni nella riga corrente */
			for (int x = 0; x < localB.X; x++) {                  // per ogni x da 0 a X-1     (0 - 4  ->  j=0: 0,1,2,3,4 -> j=1:1,2,3,4,5 -> j=2:2,3,4,5,6)
			CXCellState cellState = C[i][j + x];          // controlla il giocatore di appartenenza di ogni cella e incrementa il contatore relativo

			if (cellState == CXCellState.P1)
				countPlayer++;
			else if (cellState == CXCellState.FREE)
				countFree++;
			else
				countOpponent++;
			}

			// valuta la riga basandosi sul numero di celle 
			if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
			evalRow += countPlayer * countPlayer * countPlayer;             // incrementa il valore della riga
			else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
			evalRow -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della riga

			// bonus se ci sono celle libere nella riga
			evalRow += countFree;
		}
		}

		// Valutazione colonne
		for (int i = 0; i <= localB.M - localB.X; i++) {
		for (int j = 0; j < localB.N; j++) {
			int countPlayer = 0;    // celle del giocatore  
			int countOpponent = 0;  // celle dell'avversario
			int countFree = 0;      // celle libere

			/* analizza tutte le possibili sequenze di 5 gettoni nella colonna corrente */
			for (int x = 0; x < localB.X; x++) {
			CXCellState cellState = C[i + x][j];    // controlla il giocatore di appartenenza di ogni cella e incrementa il contatore relativo

			if (cellState == CXCellState.P1)
				countPlayer++;
			else if (cellState == CXCellState.FREE)
				countFree++;
			else
				countOpponent++;
			}

			// valuta la colonna basandosi sul numero di celle 
			if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
			evalCol += countPlayer * countPlayer * countPlayer;             // incrementa il valore della colonna
			else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
			evalCol -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della colonna

			// bonus se ci sono celle libere nella colonna
			evalCol += countFree;
		}
		}

		// Valutazione diagonali  (suppongo M=7,N=7,X=5)
		for (int i = 0; i <= localB.M - localB.X; i++) {        // righe da 0 a M-X       (0 - 7-5=2)
		for (int j = 0; j <= localB.N - localB.X; j++) {      // colonne da 0 a N-X     (0 - 7-5=2)
			int countPlayer = 0;    // celle del giocatore  
			int countOpponent = 0;  // celle dell'avversario
			int countFree = 0;      // celle libere

			/* analizza tutte le possibili sequenze di 5 gettoni diagonali sulla matrice */
			for (int x = 0; x < localB.X; x++) {          // per ogni x da 0 a X-1     (0 - 4  ->  j=0:0,1,2,3,4 -> j=1:1,2,3,4,5 -> j=2:2,3,4,5,6
														//                                       i=0:0,1,2,3,4 -> i=1:1,2,3,4,5 -> i=2:2,3,4,5,6)
			CXCellState cellState = C[i + x][j + x];

			if (cellState == CXCellState.P1)
				countPlayer++;
			else if (cellState == CXCellState.FREE)
				countFree++;
			else
				countOpponent++;
			}

			// valuta la diagonale basandosi sul numero di celle 
			if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
			evalDiag += countPlayer * countPlayer * countPlayer;             // incrementa il valore della diagonale
			else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
			evalDiag -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della diagonale

			// bonus se ci sono celle libere nella diagonale
			evalDiag += countFree;
		}
		}

		// Valutazione diagonali inverse  (suppongo M=7,N=7,X=5)
		for (int i = localB.X - 1; i < localB.M; i++) {         // righe da X-1 a M-1   (5-1=4 - 6)
		for (int j = 0; j <= localB.N - localB.X; j++) {      // colonne da 0 a N-X   (0 - 7-5=2)
			int countPlayer = 0;    // celle del giocatore  
			int countOpponent = 0;  // celle dell'avversario
			int countFree = 0;      // celle libere

			/* analizza tutte le possibili sequenze di 5 gettoni diagonali inverse sulla matrice */
			for (int x = 0; x < localB.X; x++) {                    // per ogni x da 0 a X-1     (0 - 4  ->  j=0:0,1,2,3,4 -> j=1:1,2,3,4,5 -> j=2:2,3,4,5,6
																	//                                       i=4:4,3,2,1,0 -> i=5:4,3,2,1,0 -> i=6:4,3,2,1,0)
			CXCellState cellState = C[i - x][j + x];

			if (cellState == CXCellState.P1)
				countPlayer++;
			else if (cellState == CXCellState.FREE)
				countFree++;
			else
				countOpponent++;
			}

			// valuta la diagonale inversa basandosi sul numero di celle 
			if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
			evalDiag += countPlayer * countPlayer * countPlayer;             // incrementa il valore della diagonale
			else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
			evalDiag -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della diagonale

			// bonus se ci sono celle libere nella diagonale
			evalDiag += countFree;
		}
		}

		// il valore totale corrisponde alla somma delle valutazioni di righe, colonne e diagonali
		int eval = evalRow + evalCol + evalDiag;

		return eval;
	}

	private int evaluateBoard(CXBoard B, int type) {
		CXCellState[][] C = B.getBoard();
		int countP1 = 0;
		int countP2 = 0;
		int countFree = 0;

		switch(type){
			case 0://Horizontal
				int outer = B.M;
				int inner = B.N - B.X;
				int offsetI = 0;
				int offsetJ = 1;
				break;
			case 1://Vertical
				int outer = B.M - B.X;
				int inner = B.N;
				int offsetI = 1;
				int offsetJ = 0;
				break;
			case 2://Diagonal
				//DECIDERE SE CONSEGNARE QUESTA EVALUATION O LA MIA
		}
	}



	

    private static boolean isValidPosition(CXBoard B, int row, int col) {
        return row >= 0 && row < B.M && col >= 0 && col < B.N;
    }

	/***************** OTHER *********************/

	public String playerName() {
		return "LStephens";
	}
}


/*
0 1 2 3 4 5 6 N
1 
2
3
4
5
M

M = number of rows
N = number of columns

C[M][N]
*/