package Shared;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;

import Server.Server;

/**
 * Class that gives the top 3 artists a specific user ID has listened to based on genre provided based on dataset.csv.
 */
public class GetTopArtistsByUserGenreQuery extends Query {
    // Query arguments
    public String userID;
    public String genre;

    // Query results
    public String[] result;

    /**
     * GetTopArtistsByUser query constructor. The client zone and number of the client sending the query,
     * as well as the arguments for the query, are all determined upon creating the query object.
     *
     * @param clientZone: the zone of the client sending the query.
     * @param clientNumber: the (address) number of the client sending the query.
     * @param userID: the userID argument for the query.
     * @param genre: the genre argument for the query.
     */
    public GetTopArtistsByUserGenreQuery(int clientZone, int clientNumber, String userID, String genre) {
        super(clientZone, clientNumber);
        this.userID = userID;
        this.genre = genre;
    }

    @Override
    public void run(String filename, Server server) {

        Scanner scanner = null;
        HashMap<String, Integer> playCounts = new HashMap<String, Integer>();

        try {
            scanner = new Scanner(new File(filename));
        } catch (Exception e) {
            System.out.println("Error: " + e);
            System.out.println("Something went wrong while trying to complete request.");
            System.exit(1);
        }

        ArrayList<String> musicIDs = new ArrayList<>();
        ArrayList<Integer> timesPlayed = new ArrayList<>();
        HashMap<String, ArrayList<String>> artists = new HashMap<>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.contains(userID) || !line.contains(genre)) { continue; }

            String[] data = line.split(",");

            // Find all artists in the data entry
            for (int i = 1; i < data.length; i++) {
                if (!data[i].startsWith("A")) { break; }

                // Update the play count for the artist found
                if (playCounts.containsKey(data[i])) {
                    playCounts.put(data[i], playCounts.get(data[i]) + 1);
                } else {
                    playCounts.put(data[i], 1);
                }
            }
            // Add info to cache:
            musicIDs.add(data[0]);

        }

        String[] topThreeArtists = new String[3];
        for (int i = 0; i < Math.min(3, playCounts.size()); i++) {
            Map.Entry<String, Integer> topEntry = null;
            for (Map.Entry<String, Integer> entry : playCounts.entrySet()) {
                topEntry = (topEntry == null || entry.getValue().compareTo(topEntry.getValue()) > 0) ? entry : topEntry;
            }
            playCounts.remove(topEntry.getKey());
            topThreeArtists[i] = topEntry.getKey();
        }
        System.err.println(topThreeArtists[0]);

        result = topThreeArtists;
    }


    private void generateCacheEntry(ArrayList<String> musicIDs, ArrayList<Integer> timesPlayed, HashMap<String, ArrayList<String>> artists, Server server){

        // Temp hashmap needed for users favouriteMusics, will consist of top music played for the genre based on GetTopArtistsByUserGenreQuery.
        HashMap<MusicProfile, Integer> tempMusicProfiles = new HashMap<>();
        // Temp user profile to be returned to cache
        UserProfile tempUserProfile = new UserProfile(userID);
        // For each musicID found
        for(int i = 0; i < musicIDs.size(); i++){
            // Create new music profile with relevant musicID and artists.
            MusicProfile tempMusicProfile = new MusicProfile(musicIDs.get(i), artists.get(i));
            // Add hashmap entry of the musicProfile with amount of times played.
            tempMusicProfiles.put(tempMusicProfile, timesPlayed.get(i));
        }
        // For queried genre: add top 3 music played.
        tempUserProfile.favoriteMusics.put(genre, tempMusicProfiles);
        // Return cache entry.
        server.addToCache(tempUserProfile);

    }



    @Override
    public String toString() {
        String s = "Top 3 artists for genre '" + genre + "' and user '" + userID + "' were [" + result[0] + ", " + result[1] + ", " + result[2] + "]. ";
        s += "(Turnaround time: " + (timeStamps[4] - timeStamps[0]) + "ms, ";
        s += "execution time: " + (timeStamps[3] - timeStamps[2]) + "ms, ";
        s += "waiting time: " + (timeStamps[2] - timeStamps[1]) + "ms, ";
s += "processed by server: " + processingServer + ")";
        return s;
    }
}
