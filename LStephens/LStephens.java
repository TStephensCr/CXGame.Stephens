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

	/* Default empty constructor */
	public LStephens() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		this.depth = 2;
		this.first = first;
		this.height = M;
		this.width = N;
		
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	/***************** MAIN FUNCTION *********************/

	public int selectColumn(CXBoard B) {
		System.err.println("-----------TURN-----------#########################################################################################################");	
        START = System.currentTimeMillis();
        Integer[] L = B.getAvailableColumns();
		int save = L[rand.nextInt(L.length)];
		/*refreshBlackList(B, L);
		System.err.println("Blacklist done");
		for(int i : L){
			save = L[rand.nextInt(L.length)]; // Save a random column
			if(!blackList[save])
				break;
		}*/

        try {
            /*int col = singleMoveWin(B, L);
            if (col != -1){
				System.err.println("Winning Column: " + col);
				return col;
			}

            col = singleMoveBlock(B, L);
            if (col != -1){
				System.err.println("Blocking Column: " + col);
				return col;
			}
			int col = -1;
			int albe = -1;
			int tmp = -1;
			for(int i : L){
				if(B.fullColumn(i))
					continue;
				B.markColumn(i);
				albe = alphaBetaSearch(B, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, L.length/2, first);
				if(albe > tmp){
					tmp = albe;
					col = i;
				}
				B.unmarkColumn();
			}
			System.err.println("Alpha-Beta Column: " + col);
            if (col != -1) {
				if(B.fullColumn(col)) {
					System.err.println("Alpha-beta search returned a full column");
					return save;
				}
				else if(!blackList[col])
					return col;
				else {
					System.err.println("blacklist caught "+col);
					return save;
				}
			} 
			else {
				System.err.println("Alpha-beta search returned -1");
				return save;
			}*/
			for(int i=0;i<width;i++){//STO PROVANDO A CREARE UNA MATRICE DI PROVA PER VEDERE SE LE EVALUATION FUNZIONANO SU QUESTA
				B.markColumn(i);
			}
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected"+save);
            return save;
        }
    }

	/***************** HELPER FUNCTIONS *********************/

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);

            if (state == myWin)
                return i; // Winning column found: return immediately

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

    private int alphaBetaSearch(CXBoard B, int depth, int alpha, int beta, int curCol, boolean first) throws TimeoutException {
		checktime();
		
		// Final depth reached or game ended
		CXGameState gameState = B.gameState();
		if (depth == 0 || !gameState.equals(CXGameState.OPEN)) {
			int eval = evaluateBoard(B);
			System.err.println("Returning evaluation: " + eval);
			System.err.println(Arrays.deepToString(B.getBoard()));
			CXCell lastMove = B.getLastMove();
			System.err.println("Last move: " + lastMove.i + " " + lastMove.j);
			return eval;
		}
		
		//explore central moves first
		int columnOrder[] = new int[width];
		for(int i = 0; i < width; i++){
			columnOrder[i] = width/2 + (1-2*(i%2))*(i+1)/2; 
		}

		if(first){
			int maxEval = Integer.MIN_VALUE;
			for(int x = 0; x < width; x++){
				if(B.fullColumn(columnOrder[x]))
					continue;
				B.markColumn(columnOrder[x]);
				System.err.println("Marked column: " + columnOrder[x]);
				int eval = alphaBetaSearch(B, depth - 1, alpha, beta, curCol, false);
				B.unmarkColumn();
				System.err.println("Unmarked column: " + columnOrder[x]);
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, eval);
				if(beta <= alpha)
					break;
			}
			return maxEval;
		}else{
			int minEval = Integer.MAX_VALUE;
			for(int x = 0; x < width; x++){
				if(B.fullColumn(columnOrder[x]))
					continue;
				B.markColumn(columnOrder[x]);
				System.err.println("Marked column: " + columnOrder[x]);
				int eval = alphaBetaSearch(B, depth - 1, alpha, beta, curCol, true);
				B.unmarkColumn();
				System.err.println("Unmarked column: " + columnOrder[x]);
				minEval = Math.min(minEval, eval);
				beta = Math.min(beta, eval);
				if(beta <= alpha)
					break;
			}
			return minEval;
		} 
	}

	/***************** EVALUATION FUNCTIONS *********************///il punteggio va cambiato direttamente quando una length viene salvata in un result

	public int evaluateBoard(CXBoard B) {
        int player1Score = calculatePlayerScore(B, CXCellState.P1);
        int player2Score = calculatePlayerScore(B, CXCellState.P2);

        return player1Score - player2Score;
    }

	private int calculatePlayerScore(CXBoard B, CXCellState player) {
		int playerScore = 0;
		CXCell[] MC = B.getMarkedCells();
		int centerColumn = width / 2;
		System.err.println("Entering calculatePlayerScore");
		playerScore += horizontalScore(B, player);
		
		playerScore += verticalScore(B, player);
		
		playerScore += diagonalManager(B, player);

		CXCellState[][] C = B.getBoard();

		return playerScore;
	}
//return (int) Math.pow(2, length);
    
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
					if(length > 0)
						result += length;
					if(length >= B.X)
						return 10000;
					length = 0;
				}
			}
		}
		return result;
	}

	private int verticalScore(CXBoard B, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		for(int i=0;i<width;i++){
			length = 0;
			for(int j=0;j<height;j++){
				if(C[j][i] == player)
					length++;
				else{
					if(length > 0)
						result += length;
					if(length >= B.X)
						return 10000;
					length = 0;
				}
			}
		}
		return result;
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
				if(length > 0)
					result += length;
				if(length >= B.X)
					return 10000;
				length = 0;
			}
			i--;
			j++;
		}

		return result;
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
				if(length > 0)
					result += length;
				if(length >= B.X)
					return 10000;
				length = 0;
			}
			i--;
			j--;
		}

		return result;
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
1 2 3 4 5 6 N
2
3
4
5
M

M = number of rows
N = number of columns
*/