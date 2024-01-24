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

	/* Default empty constructor */
	public LStephens() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		this.depth = 3;
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)]; // Save a random column

        try {
            int col = singleMoveWin(B, L);
            if (col != -1){
				System.err.println("Winning Column: " + col);
				return col;
			}

            col = singleMoveBlock(B, L);
            if (col != -1){
				System.err.println("Blocking Column: " + col);
				return col;
			}

			int tmp = alphaBetaSearch(B, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, 3);
			System.err.println("Alpha-Beta Column: " + tmp);
            if (tmp != -1) {
				if(B.fullColumn(tmp)) {
					System.err.println("Alpha-beta search returned a full column");
					return save;
				}
				else
					return tmp;
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

	/*private int preventForcedWin(CXBoard B, Integer[] L) throws TimeoutException {
		for(int i : L) {
			checktime();
			B.markColumn(i);
			for(int j : L) {
				checktime();
				B.markColumn(j);
				if(singleMoveBlock(B, L))
			}
		}
	}*/

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

    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {//un po' grottesco come algoritmo ma funziona
        int tmp = -1;
		boolean found = false;
		int randomVal = L[rand.nextInt(L.length)];
		int randomVal2;
		B.markColumn(randomVal); 
		for (int i : L) {
			System.err.println("Ciclo for: " + i);
			if(B.fullColumn(i))
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
			System.err.println("Random1: " + randomVal);
			System.err.println("Random2: " + randomVal2);
		}
        return tmp;
    }

    private int alphaBetaSearch(CXBoard B, int depth, int alpha, int beta, int curCol) {
		// Check if the search should stop at this depth or if the game is over
		CXGameState gameState = B.gameState();
		if (depth == 0 || !gameState.equals(CXGameState.OPEN)) {
			// Evaluate the current game state using a heuristic function
			return evaluateGameState(B);
		}

		Integer[] availableColumns = B.getAvailableColumns();

		for (int col : availableColumns) {
			// Make a move
			B.markColumn(col);

			// Recursively call alpha-beta search for the opponent's move
			int score = -alphaBetaSearch(B, depth - 1, -beta, -alpha, curCol);

			// Undo the move
			B.unmarkColumn();

			// Update alpha if we found a better move
			if (score > alpha) {
				alpha = score;
				curCol = col;
			}

			// Perform pruning
			if (alpha >= beta) {
				break;  // Beta cut-off
			}
		}

		return curCol;
	}

	private int evaluateGameState(CXBoard B) {
		int playerScore = 0;

		for (int col : B.getAvailableColumns()) {
			int row = getTopRow(col, B);  // Find the top empty row in the column

			// Check potential winning configurations
			int horizontalScore = countConsecutivePieces(B, row, col, 0, 1) + countConsecutivePieces(B, row, col, 0, -1);  // Horizontal
			int verticalScore = countConsecutivePieces(B, row, col, 1, 0);  // Vertical
			int diagonal1Score = countConsecutivePieces(B, row, col, 1, 1) + countConsecutivePieces(B, row, col, -1, -1);  // Diagonal \
			int diagonal2Score = countConsecutivePieces(B, row, col, 1, -1) + countConsecutivePieces(B, row, col, -1, 1);  // Diagonal /

			// Calculate the total score for the move
			int moveScore = horizontalScore + verticalScore + diagonal1Score + diagonal2Score;

			// Update the player's score
			if (moveScore > playerScore) {
				playerScore = moveScore;
			}
		}

		return playerScore;
	}

	private int countConsecutivePieces(CXBoard B, int row, int col, int rowIncrement, int colIncrement) {
		int consecutivePieces = 0;
		int currentRow = row;
		int currentCol = col;
		CXBoard C = B.copy();

		// Count the number of consecutive pieces in the given direction
		while (currentRow >= 0 && currentRow < C.M && currentCol >= 0 && currentCol < C.N && C.cellState(currentRow, currentCol) == C.cellState(row, col)) {
			consecutivePieces++;
			currentRow += rowIncrement;
			currentCol += colIncrement;
		}

		return consecutivePieces;
	}

	public int getTopRow(int col, CXBoard B) throws IllegalStateException {
		CXCellState[][] board = B.getBoard();
		for(int i = 0; i < B.M; i++) {
			if(board[i][col] == CXCellState.FREE) {
				return i;
			}
		}
		throw new IllegalStateException("Column " + col + " is full");
	}

	public String playerName() {
		return "LStephens";
	}
}
