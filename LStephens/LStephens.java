/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *  
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

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

/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
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
	private static final int[] WEIGHTS = {0, 1, 10, 100, 1000};

	/* Default empty constructor */
	public LStephens() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
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

	public int selectColumn(CXBoard B) {
		System.err.println("-----------TURN-----------");	
        START = System.currentTimeMillis(); // Save starting time
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
			}*/
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
			}
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected"+save);
            return save;
        }
    }

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

		System.err.println("Blocking Column found: "+found+" randomVal: "+randomVal+" randomVal2 "+randomVal2+" returns: " + tmp);
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

    private int alphaBetaSearch(CXBoard B, int depth, int alpha, int beta, int curCol, boolean first) throws TimeoutException {
		checktime();
		
		// Final depth reached or game ended
		CXGameState gameState = B.gameState();
		if (depth == 0 || !gameState.equals(CXGameState.OPEN)) {
			System.err.println("Returning evaluation: " + evaluateBoard(B));
			CXCell lastMove = B.getLastMove();
			System.err.println("Last move: " + lastMove.i + " " + lastMove.j);
			return evaluateBoard(B);
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
				int eval = alphaBetaSearch(B, depth - 1, alpha, beta, curCol, false);
				B.unmarkColumn();
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
				int eval = alphaBetaSearch(B, depth - 1, alpha, beta, curCol, true);
				B.unmarkColumn();
				minEval = Math.min(minEval, eval);
				beta = Math.min(beta, eval);
				if(beta <= alpha)
					break;
			}
			return minEval;
		} 
	}

	public int evaluateBoard(CXBoard B) {
        int player1Score = calculatePlayerScore(B, CXCellState.P1);
        int player2Score = calculatePlayerScore(B, CXCellState.P2);

        return player1Score - player2Score;
    }

	private int calculatePlayerScore(CXBoard B, CXCellState player) {
		int playerScore = 0;
		CXCell[] MC = B.getMarkedCells();
		int centerColumn = width / 2;

		for(CXCell c : MC){
			if(B.getBoard()[c.i][c.j] == player){
				// Check horizontal
				playerScore += calculateScoreInDirection(B, c.i, c.j, 0, 1, player);
				// Check vertical
				playerScore += calculateScoreInDirection(B, c.i, c.j, 1, 0, player);
				// Check diagonal (bottom-left to top-right)
				playerScore += calculateScoreInDirection(B, c.i, c.j, -1, 1, player);
				// Check diagonal (top-left to bottom-right)
				playerScore += calculateScoreInDirection(B, c.i, c.j, 1, 1, player);
			}
		}
		return playerScore;
	}

    private int calculateScoreInDirection(CXBoard B, int row, int col, int rowDirection, int colDirection, CXCellState player) {
        int length = 0;

        // Count consecutive chips in the specified direction
        while (isValidPosition(B, row, col) && B.cellState(row, col) == player) {
            length++;
            row += rowDirection;
            col += colDirection;
        }

        // Calculate the weighted score based on the length of connected chips
        return WEIGHTS[Math.min(length, WEIGHTS.length - 1)];
    }

    private static boolean isValidPosition(CXBoard B, int row, int col) {
        return row >= 0 && row < B.M && col >= 0 && col < B.N;
    }


	public String playerName() {
		return "LStephens";
	}
}
