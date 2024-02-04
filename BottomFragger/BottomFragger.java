package connectx.BottomFragger;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class BottomFragger implements CXPlayer {
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
	private static int depthMultiplier = 2;

	/* Default empty constructor */
	public BottomFragger() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		this.first = first;
		this.height = M;
		this.width = N;

		//set depth based on width to avoid certain timeout
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

		refreshBlackList(B, L);//moves that lead to enemy wins are blacklisted
		for(int i : L){
			save = L[rand.nextInt(L.length)]; // Save a random column
			if(!blackList[save])
				break;
		}
        try {

			//---Initial checks to save time---//
            int col = singleMoveWin(B, L);
            if (col != -1){
				System.err.println("Winning move found");
				return col;
			}

            col = singleMoveBlock(B, L);
            if (col != -1){
				System.err.println("Blocking move found");
				return col;
			}
			checktime();

			//---Set column checking order(middle to sides)---//
			int columnOrder[] = new int[width];
			for(int i = 0; i < width; i++){
				columnOrder[i] = width/2 + (1-2*(i%2))*(i+1)/2; 
			}

			//---Alpha-Beta Search---//
			int albe = -1;
			int bestFound = 0;
			for(int x=0; x<width; x++){
				checktime();
				if(B.fullColumn(columnOrder[x]))
					continue;
				B.markColumn(columnOrder[x]);
				albe = alphaBetaSearch(B, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, first, 1);
				if(first){
					if(albe > bestFound){
						bestFound = albe;
						col = columnOrder[x];
					}
				}
				else{
					if(albe < bestFound){
						bestFound = albe;
						col = columnOrder[x];
					}
				}
				B.unmarkColumn();
			}

			//---Result handling---//
            if (col != -1) {
				if(B.fullColumn(col)) 
					return save;
				else if(!blackList[col])
					return col;
				else{
					System.err.println("Blacklisted move selected: "+col);
					return save;
				}
			} 
			return save;

        } catch (TimeoutException e) {
			System.err.println("Timeout in selectColumn");
            return save;
        }
    }

	/***************** HELPER FUNCTIONS *********************/

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); 
            CXGameState state = B.markColumn(i);

            if (state == myWin){
				B.unmarkColumn();
                return i; 
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

		//Mark a random column and check if it leads to an enemy win
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

		//If no enemy win was found, mark another random column and check the first random column
		if(!found){
			if(L2.length <= 1)
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
			if(B.fullColumn(i))//full column before marking
				continue;
			if(B.markColumn(i) == myWin){//my win, not blacklisted
				B.unmarkColumn();
				continue;
			}
			if(B.fullColumn(i)){//my move filled the column, not blacklisted
				B.unmarkColumn();
				continue;
			}
			if(B.markColumn(i) == yourWin){//enemy moves on same column as me and wins, column blacklisted
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

		// Final depth reached or game ended
		CXGameState gameState = B.gameState();
		if (depth == 0 || !gameState.equals(CXGameState.OPEN)) {
			int eval = evaluateBoard(B,curDepth);

			if (first) {
				return (eval > alpha) ? eval : alpha;
			} else {
				return (eval < beta) ? eval : beta;
			}
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
				int eval = alphaBetaSearch(B, depth - 1, alpha, beta, false, curDepth + 1);
				B.unmarkColumn();
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, eval);
				if(beta <= alpha)
					break;
			}

			return maxEval;
		}
		else{
			int minEval = Integer.MAX_VALUE;

			for(int x = 0; x < width; x++){
				if(B.fullColumn(columnOrder[x]))
					continue;
				B.markColumn(columnOrder[x]);
				int eval = alphaBetaSearch(B, depth - 1, alpha, beta, true, curDepth + 1);
				B.unmarkColumn();
				minEval = Math.min(minEval, eval);
				beta = Math.min(beta, eval);
				if(beta <= alpha)
					break;
			}

			return minEval;
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
			int player1Score = calculatePlayerScore(B, CXCellState.P1);
			int player2Score = calculatePlayerScore(B, CXCellState.P2);

			int depthBonus = (maxDepth[depthIndex] - curDepth)*depthMultiplier;
			player1Score += depthBonus;
			player2Score += depthBonus;

			return player1Score - player2Score;
		}catch(TimeoutException e){
			System.err.println("Timeout in evaluateBoard");
			return 0;
		}
    }

	private int calculatePlayerScore(CXBoard B, CXCellState player) throws TimeoutException{
		int playerScore = 0;
		try{
			checktime();

			playerScore += centerCheck(B, player);

			playerScore += horizontalScore(B, player);
			
			playerScore += verticalScore(B, player);
			
			playerScore += diagonalFinder(B, player);

			return playerScore;
		}catch(TimeoutException e){
			System.err.println("Timeout in calculatePlayerScore");
			return 0;
		}
	}

	private int centerCheck(CXBoard B, CXCellState player){
		int playerScore = 0;
		CXCell[] MC = B.getMarkedCells();
		int centerColumn = width / 2;
		int distanceToCenter;

		for(CXCell c : MC) {
			if(B.getBoard()[c.i][c.j] == player){
				distanceToCenter= Math.abs(centerColumn - c.j);
				playerScore += (width - distanceToCenter) + 1;
			}
		}
			
		return playerScore;
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
				if(length > 1)
					result += length;
				if(length >= B.X)
					return 1000;
				length = 0;
			}
		}
		if(length > 1)
			result += length;
		if(length >= B.X)
			return 1000;
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

	private int diagonalFinder(CXBoard B, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;

		//starting from bottom row
		for(int col = width-1; col>=0; col--){
			result += diagonalUpLeft(B, height-1, col, player);
			result += diagonalUpRight(B, height-1, col, player);
		}

		//starting from right column
		for(int row = height-2; row>=(B.X-1); row--){
			result += diagonalUpLeft(B, row, width-1, player);
		}

		//starting from left column
		for(int row = height-2; row>=(B.X-1); row--){
			result += diagonalUpRight(B, row, 0, player);
		}

		return result;
	}

	private int diagonalUpRight(CXBoard B, int row, int col, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		int i = row;
		int j = col;
		while(isValidPosition(B, row, col)){
			if(C[row][col] == player)
				length++;
			else{
				if(C[row][col] != CXCellState.FREE)
					if(length > 2)
						length--;
				if(length >= (B.X-1))//dato che controllo anche le diagonali troppo corte per essere vincenti, ignoro i risultati minori di X-1
					result += length;
				if(length >= B.X)
					return 1000000;
				length = 0;
			}
			row--;
			col++;
		}
		if(length >= (B.X-1))
			result += length;
		if(length >= B.X)
			return 1000000;
		
		return result*(result-1);
	}

	private int diagonalUpLeft(CXBoard B, int row, int col, CXCellState player) {
		CXCellState[][] C = B.getBoard();
		int length = 0;
		int result = 0;
		int i = row;
		int j = col;
		while(isValidPosition(B, row, col)){
			if(C[row][col] == player)
				length++;
			else{
				if(C[row][col] != CXCellState.FREE)
					if(length > 2)
						length--;
				if(length >= (B.X-1))
					result += length;
				if(length >= B.X)
					return 1000000;
				length = 0;
			}
			row--;
			col--;
		}
		if(length >= (B.X-1))
			result += length;
		if(length >= B.X)
			return 1000000;
		
		return result*(result-1);
	}	

    private static boolean isValidPosition(CXBoard B, int row, int col) {
        return row >= 0 && row < B.M && col >= 0 && col < B.N;
    }

	/***************** OTHER *********************/

	public String playerName() {
		return "BottomFragger";
	}
}
