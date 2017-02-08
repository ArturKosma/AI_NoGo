package com.lucek.androidgameengine2d.controllers;

import android.graphics.Point;
import android.util.Log;
import com.lucek.androidgameengine2d.game.Field;
import com.lucek.androidgameengine2d.gameplay.Game;
import java.util.ArrayList;
import java.util.List;

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

        // Container for the enemy colour.
        Field enemyColour;

        // Get enemy colour.
        if(GetColour() == Field.WHITE)
        {
            enemyColour = Field.BLACK;
        }
        else
        {
            enemyColour = Field.WHITE;
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
            // Iterate through all possible points.
            for(int x = 0; x < possiblePoints.size(); x++)
            {
                // Add value of simulation at specific point to an array of values.
                moveValues[x] += Simulate(possiblePoints.get(x), enemyColour, mapCopy);

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
    private int Simulate(Point point, Field enemyColour, Field[][] mapCopy)
    {
        // Create a new map for simulation.
        Field[][] mapSimulation = mapCopy;

        // Put a pin on a possible point.
        mapSimulation[point.x][point.y] = GetColour();

        // Creates our own game instance for simulation.
        Game gameSimulation = new Game(new RandomMoveAI(10000), enemyColour, new RandomMoveAI(10000), GetColour(), mapSimulation);

        // Until simulation is over.
        while(true)
        {
            PrintCurrentMapStateToConsole(gameSimulation);
            try
            {
                gameSimulation.Update();
                Log.d("Game State", "-----U P D A T E-----");
            }
            catch(Game.GameIsOverException gameOverException)
            {
                if(gameOverException.winner == GetColour())
                {
                    Log.d("Game State", "-----W I N N E R-----");
                    return 1;
                }
                else
                {
                    Log.d("Game State", "-----L O S E R-----");
                    return 0;
                }
            }
            catch(Game.InvalidMoveException invalidMoveException)
            {
                // Never used.
            }
        }
    }

    // Prints the current map state to the console.
    private void PrintCurrentMapStateToConsole(Game simulatedGame)
    {
        String row = "";
        int rowNumber = 1;

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
