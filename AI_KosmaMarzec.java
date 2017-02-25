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
import java.util.concurrent.TimeUnit;

public class AI_KosmaMarzec extends AbstractPlayerController
{
    int[][] valuesMap;
    List<Point> possiblePoints;
    Field enemyColour, ourColour;
    List<Point> emptyPoints;
    int[][] moveValues;
    int[][] simulationsCounts;

    public AI_KosmaMarzec(long movementTime)
    {
        super(movementTime);

        valuesMap = new int[9][9];
        moveValues = new int[9][9];
        simulationsCounts = new int[9][9];

        SetupValuesMap();

        emptyPoints = new ArrayList<>();

        //Field[][] mapCopy = GetBoardState();

        for(int x = 0; x < 9; x++)
        {
            for(int y = 0; y < 9; y++)
            {
                emptyPoints.add(new Point(x,y));
            }
        }
    }

    @Override
    public Point MakeMove(Point lastOpponentMove){
        // Get copy of the map.
        Field[][] mapCopy = GetBoardState();

        if(emptyPoints.size() < 81)
            emptyPoints.remove(lastOpponentMove);

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

        //emptyPoints = new ArrayList<>();

        //reset moves values and counts
        for(int x = 0; x < 9; x++)
        {
            for(int y = 0; y < 9; y++)
            {
                moveValues[x][y] = 0;
                simulationsCounts[x][y] = 0;
            }
        }

        // Array of possible points.
        possiblePoints = new ArrayList<>();

        // Fill the array of possible points.
        for (Point move:emptyPoints)
        {
            if (IsMoveValid(move)) {
                possiblePoints.add(move);
            }
        }

        UpdateValuesMap(lastOpponentMove, enemyColour);

        // Best point container.
        Point bestMove = new Point(0, 0); // Default point.

        // Value container.
        //int[] moveValues = new int[possiblePoints.size()];
        /*moveValues = new ArrayList<>();
        simulationsCounts = new ArrayList<>();
        for (Point move:possiblePoints) {
            moveValues.add(0);
            simulationsCounts.add(0);
        } */

        // Best value buffer.
        float bestValue = 0; // Default value.

        if(emptyPoints.size() > 30) //valuation
        CutPossibleMoves((int)(1.9f + (6 - 6 * (float)emptyPoints.size() / 81)));

        //Log.d("Time Left", GetRemainingTimeMS() + "");


        //int cutDeferenceValue = 6;
        int possibleSize = possiblePoints.size();

        Point currentMove = new Point(0,0);

        // While we still have time left.
        do
        {
            //Log.d("Simulation", "S T A R T");

            // Iterate through all possible points.
            for(int x = 0; x < possibleSize; x++)
            {
                currentMove = possiblePoints.get(x);

                // Add value of simulation at specific point to an array of values.
                UpdateMoveValue(currentMove, Simulate(currentMove, enemyColour, ourColour, mapCopy));

                //Log.d("move value: ", "" + moveValues[currentMove.x][currentMove.y]/(float)simulationsCounts[currentMove.x][currentMove.y]);
                //Log.d("simulations count: ", "" + simulationsCounts[currentMove.x][currentMove.y]);

                float currentMoveValue = moveValues[currentMove.x][currentMove.y]/(float)simulationsCounts[currentMove.x][currentMove.y];

                // Update the value buffer and the best point container.
                if(currentMoveValue > bestValue)
                {
                    bestValue = currentMoveValue;
                    bestMove = possiblePoints.get(x);
                }
            }

            //Extra cutting nie dziala odpowiednio po zmianie wartosci na usrednione wiec wypieprzamy narazie
            int allValues = 0;
            int allCounts = 0;

            for (int i = 0; i < possiblePoints.size(); i++) //Extra Cutting.
            {
                allValues += moveValues[possiblePoints.get(i).x][possiblePoints.get(i).y];
                allCounts += simulationsCounts[possiblePoints.get(i).x][possiblePoints.get(i).y];
            }

            float avarageValue = allValues / (float)allCounts;

            //if((GetRemainingTimeMS() < 0))
            //Log.d("avarage value: ", "" + avarageValue);


            for (int i = 0; i < possiblePoints.size(); i++) //Extra Cutting.
            {
                if(moveValues[possiblePoints.get(i).x][possiblePoints.get(i).y]/(float)simulationsCounts[possiblePoints.get(i).x][possiblePoints.get(i).y] < avarageValue - (0.3f - 0.3f * possiblePoints.size() / 81))
                {
                    //moveValues.remove(i);
                    possiblePoints.remove(i);
                    i--;
                }
            }

            //if((GetRemainingTimeMS() < 0))
            //Log.d("moves count: ", "" + possiblePoints.size());

            //nie pamietam czy to jest potrzebne poza Extra Cutem. Chyba można wyjebac ale nie jestem pewien xd
            if(possibleSize > possiblePoints.size())
            {
                possibleSize = possiblePoints.size();

                //cutDeferenceValue ++;

                //Log.d("cutDeferenceValue", cutDeferenceValue + "");
            }



            //Log.d("Time Left", GetRemainingTimeMS() + "");
            //Log.d("Simulation", "best move: " + bestMove);
        }while((GetRemainingTimeMS() >= 0));


        UpdateValuesMap(bestMove, ourColour);

        emptyPoints.remove(bestMove);

        ///Log.d("Is This Move Fine? ", IsMoveValid(bestMove)? "Yes" : "No" );

        /*PrintCurrentMapStateToConsole(); //do usuniecia

        try {
            TimeUnit.SECONDS.sleep(1);   //do usuniecia
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        // Returns the best move found during specified time.
        return bestMove;
    }

    // Simulates the game at specific point.
    private int Simulate(Point point, Field enemyColour, Field ourColour, Field[][] mapCopy)
    {
        //Log.d("Time befor sim", GetRemainingTimeMS() + "");

        // Boolean deciding who's having the move in simulation.
        boolean enemy = true;

        // Create the random generator object.
        Random randomIndex = new Random();

        // Create a new map for simulation.
        Field[][] mapSimulation = new Field[mapCopy.length][mapCopy[0].length];

        //System.arraycopy( mapCopy, 0, mapSimulation, 0, mapCopy.length );

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
        List<Point> chosenMoves = new LinkedList<>();

        // Fill lists of possible first moves for this simulation.
        for (Point move : emptyPoints)
        {
            enemyPossibleMoves.add(move);
            ourPossibleMoves.add(move);
        }

        Field currentPlayer = Field.BLACK;

        int countMoves = 0;

        // Container for a proper index from the possible moves lists.
        int properIndex = 0;

        // Until simulation is over.
        while(true)
        {
            //Sim end
            if(++countMoves > 5)
            {
                //AgresiveAvreging
                //RemoveUnmovable(gameSimulation, ourPossibleMoves, ourColour);
                //RemoveUnmovable(gameSimulation, enemyPossibleMoves, enemyColour);
                //Log.d("Time after sim", GetRemainingTimeMS() + "");
                int currentMoveValue = ourPossibleMoves.size() - enemyPossibleMoves.size();

                /////////Tu trzeba zupować chosen move alpha-cos
                for (Point move:chosenMoves)
                {
                    UpdateMoveValue(move, currentMoveValue);
                }

                return currentMoveValue;
            }

            // Enemy move.
            if(enemy)
            {
                if(SimulateSingleMove(gameSimulation, randomIndex, mapSimulation, enemyPossibleMoves, enemyColour).x < 0) //checks if there is no more possible moves
                    countMoves = 5;
            }

            // Our move.
            else
            {
                Point chosenMove = SimulateSingleMove(gameSimulation, randomIndex, mapSimulation, ourPossibleMoves, ourColour);

                if(chosenMove.x < 0)
                    countMoves = 5;
                else
                    chosenMoves.add(chosenMove);
            }

            // Switch player.
            enemy = !enemy;
        }

    }

    void UpdateMoveValue(Point move, int newMoveValue)
    {
        simulationsCounts[move.x][move.y] += 1;
        moveValues[move.x][move.y] += newMoveValue;

        //moveValues[move.x][move.y] *= (simulationsCounts[move.x][move.y] - 1) / simulationsCounts[move.x][move.y];
        //moveValues[move.x][move.y] += newMoveValue * ( 1 / simulationsCounts[move.x][move.y]);
    }

    Point SimulateSingleMove(Game gameSimulation, Random randomIndex, Field[][] mapSimulation, List<Point> possibleMoves, Field color)
    {
        // Search for a proper index.
        int properIndex = 0;

        while(true)
        {
            properIndex = randomIndex.nextInt(possibleMoves.size());

            // Delete unproper index.
            if (!gameSimulation.IsMoveValid(possibleMoves.get(properIndex), color))
            {
                possibleMoves.remove(properIndex);

                // Enemy lose.
                if (possibleMoves.size() == 0)
                {
                    return new Point(-1,-1); //taa wiem ze messy ale tak jest najsensowniej
                }
            }

            else
            {
                // Put a pin on that location.
                mapSimulation[possibleMoves.get(properIndex).x][possibleMoves.get(properIndex).y] = color;
                return possibleMoves.get(properIndex);
            }
        }

    }

    void RemoveUnmovable(Game simulation, List<Point> moves, Field color)
    {
        for (int i = 0; i < moves.size(); i++)
        {
            if(!simulation.IsMoveValid(moves.get(i), color))
            {
                moves.remove(i--);
            }
        }
    }

    void SetupValuesMap() //w konstruktorze
    {
        for(int y = 0; y < valuesMap[0].length; y++)
            for(int x = 0; x < valuesMap.length; x++)
            {
                valuesMap[x][y] = 0;
                if(x==0 || x==valuesMap.length-1)
                {
                    valuesMap[x][y] += 2;
                }
                if(y==0 || y==valuesMap.length-1)
                {
                    valuesMap[x][y] += 2;
                }
            }
    }

    void UpdateValuesMap(Point point, Field color) //point - pozycja ostatniego ruchu, color - kolor ostatniego ruchu
    {
        if(color == GetColour())
        {
            //zmiejsza wartość na przyległych do naszego
            AddValueToValuesMapStraight(point, -1, 1);

            //zmniejsza wartość na oddalonych o 2 od naszego
            AddValueToValuesMapStraight(point, -2, 2);
            AddValueToValuesMapSlant(point, -2, 1);
        }
        else
        {
            //zwiększa wartość na przyległych do przeciwnika
            AddValueToValuesMapStraight(point, 1, 1);

            //zmiejsza wartość na oddalonych o 2 od przeciwnika
            AddValueToValuesMapStraight(point, -1, 2);
            AddValueToValuesMapSlant(point, -1, 1);
        }
    }

    void CutPossibleMoves(int untrust) //im mniejszy untrust tym więcej ucina. Dla 0 ucina wszystkie mniejsze od maksymalnej wartosci
    {
        int valueMax = -1000;
        int valueSecondMax = -1000;

        //znalezienie najwiekszej wartosci ruchu

        for (Point move:possiblePoints)
        {
            if(valuesMap[move.x][move.y] >= valueMax)
            {
                valueSecondMax = valueMax;
                valueMax = valuesMap[move.x][move.y];
            }
        }

        //Log.d("Max Value", valueMax + "");

        //Log.d("moves befor", possiblePoints.size() + "");

        //usunięcie ruchów o wartości mniejszej od maksymalnej o więcej niż parametr nieufności untrust
        for(int i = 0; i < possiblePoints.size(); i++)
        {
            if(valuesMap[possiblePoints.get(i).x][possiblePoints.get(i).y] < valueSecondMax - untrust)
            {
                possiblePoints.remove(i);
                i--;
            }
        }

        //Log.d("moves after", possiblePoints.size() + "");
    }

    void AddValueToValuesMapStraight(Point point, int value, int range)
    {
        if(point.x - range >= 0 )
        {
            valuesMap[point.x - range][point.y] += value;
        }

        if(point.x + range <= valuesMap.length - 1 )
        {
            valuesMap[point.x + range][point.y] += value;
        }

        if(point.y - range >= 0 )
        {
            valuesMap[point.x][point.y - range] += value;
        }

        if(point.y + range <= valuesMap.length - 1 )
        {
            valuesMap[point.x][point.y + range] += value;
        }
    }

    void AddValueToValuesMapSlant(Point point, int value, int range)
    {
        if(point.x - range >= 0 )
        {
            if(point.y - range >= 0 )
            {
                valuesMap[point.x - range][point.y - range] += value;
            }

            if(point.y + range <= valuesMap.length - 1 )
            {
                valuesMap[point.x - range][point.y + range] += value;
            }
        }

        if(point.x + range <= valuesMap.length - 1 )
        {
            if(point.y - range >= 0 )
            {
                valuesMap[point.x + range][point.y - range] += value;
            }

            if(point.y + range <= valuesMap.length - 1 )
            {
                valuesMap[point.x + range][point.y + range] += value;
            }
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

    private void PrintCurrentMapStateToConsole()
    {
        String row = "";
        int rowNumber = 0;

        // Fills the row string.
        for(int i = 0; i < GetBoardState().length; i++)
        {
            for(int n = 0; n < GetBoardState().length; n++)
            {
                if(GetBoardState()[n][i] == Field.WHITE)
                {
                    row += 'W';
                }

                else if(GetBoardState()[n][i] == Field.BLACK)
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