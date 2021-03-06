package AES;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class Keys {
    private int[][] currentKeyMatrix;
    protected String initialKeyState;
    protected int algorithm, maxColumnNo;
    private final int[] RCON = { 1, 2, 4, 8, 16, 32, 64, 128, 27, 54 };
    private int[][][] keyMatrix;
    private SBox S = new SBox();

    public String getInitialKeyState() {
        return initialKeyState;
    }// end of String getInitKeyState()

    /**
     * KEYS - (CONSTRUCTOR) 
     * This constructor takes 3 parameters - a seed for
     * generating the key, a salt for improving randomness and the algorithm variant
     * which specifies the length of the key to be generated. Spits out the
     * pseudorandom key.
     *
     * @param seed      - String to generate the key
     * @param salt      - String to improve the randomness of the key
     * @param algorithm - specifies the length of the key (128, 192 or 256)
     * @return - the 4x4 resultant matrix.
     */
    public Keys(String seed, String salt, int algorithm) {
        // STEP 1 : GENERATE INITIAL KEY STATE FROM THE SEED AND SALT
        SecretKey key = null;
        try {
            KeySpec keyGen = new PBEKeySpec(seed.toCharArray(), salt.getBytes(), 65536, algorithm);
            key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(keyGen);
        } // try block
        catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            System.out.println("ERROR");
            System.exit(0);
        } // catch block
        initialKeyState = ByteArrayToHexadecimal(key.getEncoded());

        // STEP 2 : PROPERLY INITIALIZE THE ALGORITHM AND KEY MATRIX
        switch (algorithm) {
            case 128:
                currentKeyMatrix = new int[4][4];
                this.algorithm = 128;
                maxColumnNo = 3;
                break;

            case 192:
                currentKeyMatrix = new int[4][6];
                this.algorithm = 192;
                maxColumnNo = 5;
                break;

            case 256:
                currentKeyMatrix = new int[4][8];
                this.algorithm = 256;
                maxColumnNo = 7;
                break;
        }// switch statement
    }// end of public Keys(String, String, int)

    /**
     * KEYS - GENERATE KEY MATRIX
     * This function takes a single parameter - the round
     * number. It uses the global variable - currentKeyMatrix in conjunction with
     * the round number to generate the corresponding key for the specified round
     * based on the key generation procdures specified in Rjindael.
     *
     * @param roundNo - specifies the round no.
     * @return - the 4x4 resultant matrix.
     */
    protected int[][] generateKeyMatrix(int roundNo) {
        int[][] outputMatrix = new int[4][4];
        // STEP 1 : TAKE CARE OF THE KEY0 STATE FOR ALL POSSIBLE KEY LENGTHS
        if (roundNo == 0) {
            for (int i = 0; i < initialKeyState.length(); i += 2)
                currentKeyMatrix[(i / 2) % 4][Math.floorDiv((i / 2), 4)] = Integer
                        .parseInt(initialKeyState.substring(i, i + 2), 16);
            for (int i = 0; i < 4; i++)
                System.arraycopy(currentKeyMatrix[i], 0, outputMatrix[i], 0, 4);
            return outputMatrix;
        } // if statement - INITIAL ROUND

        // STEP 2: HANDLE THE KEY GENERATION PER ALGORITHM
        int[] column = new int[4];
        switch (algorithm) {
            // STEP 2.1 - 128 BIT VARIANT
            case 128:
                // STEP 2.1.1 : ASSIGN THE LAST COLUMN (COLUMN 3) TO A TEMPORARY VARIABLE.
                for (int i = 0; i < 4; i++)
                    column[i] = currentKeyMatrix[i][3];

                // STEP 2.1.2 : GENERATE THE FIRST COLUMN OF THE NEXT KEY MATRIX
                column = functionF(column, roundNo - 1);
                for (int i = 0; i < 4; i++)
                    currentKeyMatrix[i][0] ^= column[i];

                // STEP 2.1.3 : USE THE FIRST COLUMN TO GENERATE THE NEXT COLUMNS
                for (int c = 1; c < 4; c++)
                    for (int r = 0; r < 4; r++)
                        currentKeyMatrix[r][c] ^= currentKeyMatrix[r][c - 1];
                break;

            // STEP 2.2 - 192 BIT VARIANT
            case 192:
                switch (roundNo % 3) {
                    case 0:
                        // STEP 2.2.0.1 : ASSIGN THE LAST COLUMN (COLUMN 5) TO A TEMPORARY VARIABLE.
                        for (int i = 0; i < 4; i++)
                            column[i] = currentKeyMatrix[i][5];

                        // STEP 2.2.0.2 : GENERATE THE FIRST COLUMN OF THE NEXT 4X6 KEY MATRIX
                        column = functionF(column, roundNo - Math.floorDiv(roundNo, 3) - 1);
                        for (int r = 0; r < 4; r++)
                            currentKeyMatrix[r][0] ^= column[r];

                        // STEP 2.2.0.3 : USE THE 0th COLUMN TO GENERATE COLUMNS THE NEXT 3 COLUMNS
                        for (int c = 1; c < 4; c++)
                            for (int r = 0; r < 4; r++)
                                currentKeyMatrix[r][c] ^= currentKeyMatrix[r][c - 1];
                        break;

                    case 1:
                        // STEP 2.2.1.1 : GENERATE THE COLUMNS 4 & 5
                        if (roundNo != 1) {
                            for (int c = 4; c < 6; c++)
                                for (int r = 0; r < 4; r++)
                                    currentKeyMatrix[r][c] ^= currentKeyMatrix[r][c - 1];
                        } // if statement - ROUND 1 EXCEPTION

                        // STEP 2.2.1.2 : ASSIGN THE LAST COLUMN TO A TEMPORARY VARIABLE.
                        for (int i = 0; i < 4; i++)
                            column[i] = currentKeyMatrix[i][5];

                        // STEP 2.2.1.3 : GENERATE THE FIRST COLUMN OF THE NEXT KEY 4X6 MATRIX
                        column = functionF(column, roundNo - Math.floorDiv(roundNo, 3) - 1);
                        for (int r = 0; r < 4; r++)
                            currentKeyMatrix[r][0] ^= column[r];

                        // STEP 2.2.1.4 : USE THE 0th COLUMN TO GENERATE COLUMN 1
                        for (int r = 0; r < 4; r++)
                            currentKeyMatrix[r][1] ^= currentKeyMatrix[r][0];

                        // STEP 2.2.1.5 : ISOLATE THE COLUMNS 4,5,0,1 INTO A 4x4 PART TO RETURN
                        for (int i = 4; i <= 7; i++)
                            for (int j = 0; j < 4; j++)
                                outputMatrix[j][i - 4] = currentKeyMatrix[j][i % 6];
                        return outputMatrix;

                    case 2:
                        // STEP 2.2.2.1 : GENERATE COLUMNS 2,3,4,5
                        for (int c = 2; c <= 5; c++)
                            for (int r = 0; r < 4; r++)
                                currentKeyMatrix[r][c] ^= currentKeyMatrix[r][c - 1];

                        // STEP 2.2.2.2 : ISOLATE COLUMNS 2,3,4,5 INTO A 4x4 PART
                        for (int i = 2; i <= 5; i++)
                            for (int j = 0; j < 4; j++)
                                outputMatrix[j][i - 2] = currentKeyMatrix[j][i];
                        return outputMatrix;
                }// switch statement - ROUND NO.
                break;

            // STEP 2.3 - 256 BIT VARIANT
            case 256:
                switch (roundNo % 2) {
                    case 0:
                        // STEP 2.3.0.1 : ASSIGN THE LAST COLUMN (COLUMN 7) TO A TEMPORARY VARIABLE.
                        for (int i = 0; i < 4; i++)
                            column[i] = currentKeyMatrix[i][7];

                        // STEP 2.3.0.2 : GENERATE THE FIRST COLUMN OF THE NEXT KEY MATRIX
                        column = functionF(column, (roundNo / 2) - 1);

                        for (int r = 0; r < 4; r++)
                            currentKeyMatrix[r][0] ^= column[r];

                        // STEP 2.3.0.3 : USE THE FIRST COLUMN TO GENERATE THE NEXT COLUMNS
                        for (int c = 1; c < 4; c++)
                            for (int r = 0; r < 4; r++)
                                currentKeyMatrix[r][c] ^= currentKeyMatrix[r][c - 1];
                        break;

                    case 1:
                        // STEP 2.3.1.1 : ASSIGN COLUMN 3 TO A TEMPORARY VARIABLE.
                        for (int i = 0; i < 4; i++)
                            column[i] = currentKeyMatrix[i][3];

                        // STEP 2.3.1.2 : GENERATE COLUMN 4 USING COLUMN 3.
                        if (roundNo != 1) {
                            for (int r = 0; r < 4; r++)
                                currentKeyMatrix[r][4] ^= S.performSubstitution(currentKeyMatrix[r][3], 'e');

                            // STEP 2.3.1.3 : USE THE COLUMN 4 TO GENERATE COLUMNS 5,6,7
                            for (int c = 5; c <= 7; c++)
                                for (int r = 0; r < 4; r++)
                                    currentKeyMatrix[r][c] ^= currentKeyMatrix[r][c - 1];
                        } // if statement - ROUND 1 EXCEPTION

                        // STEP 2.3.1.4 : ISOLATE THE COLUMNS 4,5,0,1 INTO A 4x4 PART TO RETURN
                        for (int i = 4; i <= 7; i++)
                            for (int j = 0; j < 4; j++)
                                outputMatrix[j][i - 4] = currentKeyMatrix[j][i];
                        return outputMatrix;
                }// switch statement - ROUND NO.
                break;
        }// switch statement - KEY SIZE

        // STEP 3 : ISOLATE COLUMNS 0,1,2,3 INTO A 4x4 TO RETURN
        for (int i = 0; i < 4; i++)
            System.arraycopy(currentKeyMatrix[i], 0, outputMatrix[i], 0, 4);
        return outputMatrix;
    }// end of int[][] generateKeyMatrix(int)

    /**
     * KEYS - GENERATE INVERSE KEY MATRIX 
     * This function takes a single parameter -
     * the total number of rounds. It constructs a 3D matrix of dimensions (rounds +
     * 1) x 4 x 4 where the ith 2D matrix corresponds to the (i+1)th round of the
     * key generation protocol. Stores this in the keyMatrix global variable.
     * Returns nothing.
     *
     * @param noOfRounds - the total number of rounds.
     */
    protected void generateInverseKeyMatrices(int noOfRounds) {
        // Step 1 : INITIALIZE KEYMATRIX ARRAY
        keyMatrix = new int[noOfRounds + 1][4][4];

        // STEP 2 : GENERATE KEY MATRICES AND STORE IN KEYMATRIX[][][]
        for (int round = 0; round <= noOfRounds; round++)// generating the key for the round (roundNo-1)
            keyMatrix[round] = generateKeyMatrix(round);
    }// end of void generateInverseKeyMatrices(int)

    /**
     * KEYS - GET KEY MATRIX 
     * This function takes a single parameter - the round
     * number and extracts and returns the the corresponding key from the keyMatrix
     * global variable.
     *
     * @param roundNo - the round number
     * @return - the key matrix for the specified round number.
     */
    protected int[][] getKeyMatrix(int roundNo) {
        return keyMatrix[roundNo];
    }// end of int[][] getKeyMatrix(int)

    /**
     * KEYS - F FUNCTION 
     * This function takes 2 parameters - a column array and the
     * specific RCON number It returns the resultant column after passing through
     * the F Function as specified in the Rjindael specification
     *
     * @param column - the column to process.
     * @param rconNo - the round number
     * @return - the column after passing through the F Function
     */
    private int[] functionF(int[] column, int rconNo) {
        int[] resultantKeyColumn = new int[4];

        // STEP 1: ROTWORD
        resultantKeyColumn[3] = column[0];
        System.arraycopy(column, 1, resultantKeyColumn, 0, 3);

        // STEP 2 : SUBBYTES
        for (int i = 0; i < 4; i++)
            resultantKeyColumn[i] = S.performSubstitution(resultantKeyColumn[i], 'e');

        // STEP 3 : RCON
        resultantKeyColumn[0] ^= RCON[rconNo];
        return resultantKeyColumn;
    }// end of int[] functionF(int[], int)

    /**
     * KEYS - BYTE ARRAY TO HEX 
     * This function takes a single parameters - a byte
     * array and outputs the hex value of each of the elements of the array
     * concatenated together.
     *
     * @param input - the byte array
     * @return - the concatenated hex value
     */
    private String ByteArrayToHexadecimal(byte[] input) {
        String result = "";
        for (byte b : input)
            result += String.format("%02X", b);
        return result;
    }// end of String ByteArrayToHexadecimal(byte[])
}// end of class
