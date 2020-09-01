package AES;

public class AESRoundFunction {

    protected static void subBytes(int[][] matrix){
        SBox S = new SBox('e');
        for (int c = 0; c < matrix.length; c++)
            for (int j = 0; j < matrix[c].length; j++)
                matrix[c][j] = S.performSubstitution(matrix[c][j]);
        displayMatrix(matrix);
    }//end of void subBytes(int[][])

    protected static void shiftRows(int[][] matrix){
        int[] tempArray;
        for (int n = 0; n < 4; n++) {
            tempArray = new int[4];
            for (int c = 0; c < 4; c++)
                tempArray[c] = matrix[n][(c+n)%4];
            matrix[n]= tempArray;            
        }//for loop - n
        displayMatrix(matrix);
    }// end of void shiftRows(int[][])

    protected static void mixColumns(int[][] matrix){
        int[] tmp = new int[4];
        for (int c = 0; c < 4; c++) {
            tmp[0] = dotProduct(2, matrix[0][c]) ^ dotProduct(3, matrix[1][c]) ^ matrix[2][c] ^ matrix[3][c];

            tmp[1]= matrix[0][c] ^ dotProduct(2, matrix[1][c]) ^ dotProduct(3, matrix[2][c]) ^ matrix[3][c];

            tmp[2] = matrix[0][c] ^ matrix[1][c] ^ dotProduct(2, matrix[2][c]) ^ dotProduct(3, matrix[3][c]);

            tmp[3] = dotProduct(3, matrix[0][c]) ^ matrix[1][c] ^ matrix[2][c] ^ dotProduct(2, matrix[3][c]);
        
            for(int r = 0 ; r < 4 ; r++)
                matrix[r][c] = tmp[r];
        }//for loop - c
        
        displayMatrix(matrix);
    }// end of void mixColumns(int[][])

    private static int dotProduct(int mulValue, int number){
        int result = 0;
        switch (mulValue) {
            case 2:
                result = number << 1 >= 256 ? (number << 1) ^ 256 : number << 1;
                result = number >= 128 ? result ^ 27 : result;
                break;

            case 3:
                result = dotProduct(2, number) ^ number;
        }//switch statement                        
        return result;
    }//end of int dotProduct(int, int)

    protected static void addRoundKey(int[][] keyMatrix, int[][] matrix) {
        for (int c = 0; c < 4; c++){
            matrix[0][c] ^= keyMatrix[0][c];
            matrix[1][c] ^= keyMatrix[1][c];
            matrix[2][c] ^= keyMatrix[2][c];
            matrix[3][c] ^= keyMatrix[3][c];
        }
        displayMatrix(matrix);                
    }// end of void addRoundKey(int[][])

    private static void displayMatrix(int[][] matrix){
        for(int r = 0 ; r < 4 ; r++){
            for(int c = 0 ; c < 4 ; c++){
                System.out.print(Integer.toHexString(matrix[r][c]) + ", ");
            }
            System.out.println();
        }        
    }
}//end of class
