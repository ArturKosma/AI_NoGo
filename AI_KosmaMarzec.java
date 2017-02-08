package com.lucek.androidgameengine2d.controllers;

import android.graphics.Point;
import android.util.Log;
import com.lucek.androidgameengine2d.game.Field;
import com.lucek.androidgameengine2d.gameplay.Game;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Artur Kosma & Michal Marzec on 07.02.2017.
 * Our alghoritm contains MCTS technique in order to find best possible moves.
 */

public class AI_KosmaMarzec extends AbstractPlayerController
{
    public AI_KosmaMarzec(long movementTime)
    {
        super(movementTime);
    }

    @Override
    public Point MakeMove(Point lastOpponentMove)
    {
        // Get copy of the map.
        Field[][] mapCopy = GetBoardState();

        // Container for the colours.
        Field enemyColour, ourColour;

        // Get enemy colour.
        if(GetColour() == Field.WHITE)
        {
            enemyColour = Field.BLACK;
            ourColour = Field.WHITE;
        }
        else
        {
            enemyColour = Field.WHITE;
            ourColour = Field.BLACK;
        }

        // Array of possible points.
        List<Point> possiblePoints = new ArrayList<>();

        // Fill the array of possible points.
        for(int x = 0; x < mapCopy.length; x++)
        {
            for(int y = 0; y < mapCopy[0].length; y++)
            {
                if(IsMoveValid(new Point(x, y)))
                {
                    possiblePoints.add(new Point(x, y));
                }
            }
        }

        // Best point container.
        Point bestMove = new Point(0, 0); // Default point.

        // Value container.
        int[] moveValues = new int[possiblePoints.size()];

        // Best value buffer.
        int bestValue = 0; // Default value.

        // While we still have time left.
        while(GetRemainingTimeMS() >= 0)
        {
            Log.d("Simulation", "S T A R T");
            // Iterate through all possible points.
            for(int x = 0; x < possiblePoints.size(); x++)
            {
                // Add value of simulation at specific point to an array of values.
                moveValues[x] += Simulate(possiblePoints.get(x), enemyColour, ourColour, mapCopy);
                Log.d("Simulation", "F I N I S H E D");

                // Update the value buffer and the best point container.
                if(moveValues[x] > bestValue)
                {
                    bestValue = moveValues[x];
                    bestMove = possiblePoints.get(x);
                }
            }
        }

        // Returns the best move found during specified time.
        return bestMove;
    }

    // Simulates the game at specific point.
    private int Simulate(Point point, Field enemyColour, Field ourColour, Field[][] mapCopy)
    {
        // Boolean deciding who's having the move in simulation.
        boolean enemy = true;

        // Create the random generator object.
        Random randomIndex = new Random();

        // Container for a proper index from the possible moves lists.
        int properIndex;

        // Create a new map for simulation.
        Field[][] mapSimulation = new Field[mapCopy.length][mapCopy[0].length];

        // Fill new map for simulation.
        for(int i = 0; i < mapSimulation.length; i++)
        {
            for(int n = 0; n < mapSimulation[0].length; n++)
            {
                mapSimulation[i][n] = mapCopy[i][n];
            }
        }

        // Put a pin on a possible point.
        mapSimulation[point.x][point.y] = GetColour();

        // Creates our own game instance for simulation.
        Game gameSimulation = new Game(new RandomMoveAI(0), enemyColour, new RandomMoveAI(0), ourColour, mapSimulation);

        // Create linked lists of possible points for both players in simulation.
        List<Point> enemyPossibleMoves = new LinkedList<>();
        List<Point> ourPossibleMoves = new LinkedList<>();

        // Fill lists of possible first moves for this simulation.
        for(int x = 0; x < mapSimulation.length; x++)
        {
            for(int y = 0; y < mapSimulation[0].length; y++)
            {
                if(gameSimulation.IsMoveValid(new Point(x, y), enemyColour))
                {
                    enemyPossibleMoves.add(new Point(x, y));
                }

                if(gameSimulation.IsMoveValid(new Point(x, y), ourColour))
                {
                    ourPossibleMoves.add(new Point(x, y));
                    ourPossibleMoves.size();
                }
            }
        }

        // Until simulation is over.
        while(true)
        {
            // Enemy move.
            if(enemy)
            {
                // Search for a proper index.
                while(true)
                {
                    // Generate random index.
                    properIndex = randomIndex.nextInt(enemyPossibleMoves.size());

                    // Delete unproper index.
                    if (!gameSimulation.IsMoveValid(new Point(enemyPossibleMoves.get(properIndex)), enemyColour))
                    {
                        enemyPossibleMoves.remove(properIndex);

                        // Enemy lose.
                        if (enemyPossibleMoves.size() == 0)
                        {
                            return 1;
                        }
                    }

                    else
                    {
                        break;
                    }
                }

                // Put a pin on that location.
                mapSimulation[enemyPossibleMoves.get(properIndex).x][enemyPossibleMoves.get(properIndex).y] = enemyColour;
            }

            // Our move.
            else
            {
                // Search for a proper index.
                while(true)
                {
                    // Generate random index.
                    properIndex = randomIndex.nextInt(ourPossibleMoves.size());

                    // Delete unproper index.
                    if (!gameSimulation.IsMoveValid(new Point(ourPossibleMoves.get(properIndex)), ourColour))
                    {
                        ourPossibleMoves.remove(properIndex);

                        // We lose.
                        if (ourPossibleMoves.size() == 0)
                        {
                            return 0;
                        }
                    }

                    else
                    {
                        break;
                    }
                }

                // Put a pin on that location.
                mapSimulation[ourPossibleMoves.get(properIndex).x][ourPossibleMoves.get(properIndex).y] = ourColour;
            }

            // Switch player.
            enemy = !enemy;
        }
    }

    // Prints the current map state to the console.
    // DEBUG ONLY.
    private void PrintCurrentMapStateToConsole(Game simulatedGame)
    {
        String row = "";
        int rowNumber = 0;

        // Fills the row string.
        for(int i = 0; i < simulatedGame.GetBoardState().length; i++)
        {
            for(int n = 0; n < simulatedGame.GetBoardState()[0].length; n++)
            {
                if(simulatedGame.GetBoardState()[i][n] == Field.WHITE)
                {
                    row += 'W';
                }

                else if(simulatedGame.GetBoardState()[i][n] == Field.BLACK)
                {
                    row += 'B';
                }

                else
                {
                    row += 'X';
                }
            }

            // Debug and clear.
            Log.d("Row number: " + rowNumber, row);
            row = "";
            rowNumber++;
        }
    }
}
