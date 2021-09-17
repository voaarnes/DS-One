package Shared;

import java.io.File;
import java.util.Scanner;

/**
 * Class that gives the number of times a userID has played a specific musicID based on the dataset.csv.
 */
public class GetTimesPlayedByUserQuery extends Query {
    // Query arguments
    public String musicID;
    public String userID;

    // Query results
    public int result;

    /**
     * GetTimesPlayedByUser query constructor. The client zone and number of the client sending the query,
     * as well as the arguments for the query, are all determined upon creating the query object.
     *
     * @param clientZone: the zone of the client sending the query.
     * @param clientNumber: the (address) number of the client sending the query.
     * @param musicID: the musicID argument for the query.
     * @param userID: the userID argument for the query.
     */
    public GetTimesPlayedByUserQuery(int clientZone, int clientNumber, String musicID, String userID) {
        super(clientZone, clientNumber);
        this.musicID = musicID;
        this.userID = userID;
    }

    @Override
    public void run(String filename) {
        Scanner scanner = null;
        int counter = 0;

        try {
            scanner = new Scanner(new File(filename));
        } catch (Exception e) {
            System.out.println("Error: " + e);
            System.out.println("Something went wrong while trying to complete request.");
            System.exit(1);
        }

        //  Scan trough entire dataset and count amount of times listened to song by userID.
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.contains(musicID) || !line.contains(userID)) { continue; }

            String[] data = line.split(",");
            counter += Integer.parseInt(data[data.length - 1]);
        }

        result = counter;
    }

    @Override
    public String toString() {
        String s = "Music '" + musicID + "' was played " + result + " times by user '" + userID + "'. ";
        s += "(Turnaround time: " + (timeStamps[4] - timeStamps[0]) + "ms, ";
        s += "execution time: " + (timeStamps[3] - timeStamps[2]) + "ms, ";
        s += "waiting time: " + (timeStamps[2] - timeStamps[1]) + "ms, ";
        s += "processed by server: " + processingServer + ")";
        return s;
    }
}
