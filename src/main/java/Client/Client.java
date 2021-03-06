package Client;

import Server.ProxyServerInterface;
import Server.ServerInterface;
import Shared.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements ClientCallbackInterface, Serializable {
    private int clientNumber;
    private Registry registry = null;

    private LinkedList<Query> responses = new LinkedList<>();
    private int sentQueries = 0;
    private LinkedList<UserProfile> cache = new LinkedList<>();

    private ProxyServerInterface proxyServer = null;
    private ServerInterface server = null;

    // Used to make sure only one server can send back a response at a time
    Lock lock = new ReentrantLock();
    
    // Variables to store average times for the different query-types
    long getTimesPlayedByUserTurnaround = 0;
    long getTimesPlayedByUserExecution = 0;
    long getTimesPlayedByUserWaiting = 0;
    long getTimesPlayedTurnaround = 0;
    long getTimesPlayedExecution = 0;
    long getTimesPlayedWaiting = 0;
    long getTopArtistsByUserGenreTurnaround = 0;
    long getTopArtistsByUserGenreExecution = 0;
    long getTopArtistsByUserGenreWaiting = 0;
    long getTopThreeMusicByUserTurnaround = 0;
    long getTopThreeMusicByUserExecution = 0;
    long getTopThreeMusicByUserWaiting = 0;

    /**
     * Constructor for client.
     *
     * @param clientNumber: unique ID for the client.
     */
    public Client(int clientNumber, int port) {
        this.clientNumber = clientNumber;
        startClient(port);
    }

    /**
     * Finds and uses the registry to lookup the proxy-server.
     */
    private void startClient(int port) {
        try {
            // Get the registry
            registry = LocateRegistry.getRegistry("localhost", port - 7);

            // Lookup the proxy-server
            proxyServer = (ProxyServerInterface) registry.lookup("proxy-server");

            // Export the client to the registry
            UnicastRemoteObject.exportObject(this, port);

            // Bind the client to the registry
            registry.bind("client_" + clientNumber, this);
        } catch (Exception e) {
            System.out.println("\nError:\n" + e);
            System.out.println("\nSomething went wrong when trying to start client_" + clientNumber + ".");
            System.exit(1);
        }
        System.out.println("client_" + clientNumber + " has started successfully.");
    }

    /**
     * Remote method invoked by the server to respond to a query already sent out by the client.
     *
     * @param response: the query object populated with a response.
     * @throws RemoteException
     */
    public void sendQueryResponse(Query response) throws RemoteException {
        lock.lock();

        // Set the final event timestamp representing that the query has been returned to the client object
        response.timeStamps[4] = System.currentTimeMillis();
        addToCache(response);
        responses.add(response);

        System.out.println("Client received query response.");
        System.out.println("Received responses: " + responses.size());

        if (responses.size() == sentQueries) {
            conclude();
        }

        lock.unlock();
    }

    /**
     * Get a server assignment from the proxy-server, parse the query and build a query object,
     * then send the query object to the server assigned by the proxy-server.
     *
     * @param queryString: the query as a string.
     * @param zone: the zone in which the client is sending the query from.
     */
    public void processQuery(String queryString, int zone) {
        // Get a server assignment from the proxy-server
        getServerAssignment(zone);

        // Parse the query
        String[] data = queryString.split("\\(");
        String method = data[0];
        String[] arguments = data[1].substring(0, data[1].length() - 1).split(",");

        // Build the query object and send the query object to the server for processing
        try {
            Query query = null;
            switch (method) {
                case "getTimesPlayed" -> {
                    assert (arguments.length == 1);
                    query = new GetTimesPlayedQuery(zone, clientNumber, arguments[0]);
                }
                case "getTimesPlayedByUser" -> {
                    assert (arguments.length == 2);
                    query = new GetTimesPlayedByUserQuery(zone, clientNumber, arguments[0], arguments[1]);
                }
                case "getTopThreeMusicByUser" -> {
                    assert (arguments.length == 1);
                    query = new GetTopThreeMusicByUserQuery(zone, clientNumber, arguments[0]);
                }
                case "getTopArtistsByUserGenre" -> {
                    assert (arguments.length == 2);
                    query = new GetTopArtistsByUserGenreQuery(zone, clientNumber, arguments[0], arguments[1]);
                }
                default -> {
                    System.out.println("\nError:\nInvalid remote method query: '" + method + "'.");
                    System.exit(1);
                }
            }

            // Finally, set the timestamp for when the query is sent from the client, then send it to the server
            query.timeStamps[0] = System.currentTimeMillis();
            server.sendQuery(query);
//            if (!searchCache(query)){
//
//            }else{
//                getFromCache(query);
//            }

            sentQueries++;
            System.out.println("Client sent query. Number of sent queries: " + sentQueries);
        } catch (Exception e) {
            System.out.println("\nError:\n" + e);
            System.out.println("\nSomething went wrong when trying to send query from client_" + clientNumber + " to " + server + ".");
            System.exit(1);
        }
    }

    /**
     *
     */
    private void addToCache(Query response){

    }

    /**
     *
     */
    private boolean searchCache(Query query){
        boolean hit = false;


        return true;
    }

    /**
     *
     */
    private void getFromCache(Query query){
        if (query instanceof GetTimesPlayedByUserQuery) {

        } else if (query instanceof GetTimesPlayedQuery) {

        } else if (query instanceof GetTopArtistsByUserGenreQuery) {

        } else if (query instanceof GetTopThreeMusicByUserQuery) {

        }
    }

    private void conclude() {
        System.out.println("Writing query responses to file ...");
        try {
            FileWriter writer = new FileWriter("src\\main\\java\\Client\\Outputs\\output_naive.txt");

            while (responses.size() != 0){
                Query response = responses.remove();
                writer.write(response.toString() + "\n");

                // Add the query's turnaround, execution and waiting time to the average statistics
                long turnaround = response.timeStamps[4] - response.timeStamps[0];
                long execution = response.timeStamps[3] - response.timeStamps[2];
                long waiting = response.timeStamps[2] - response.timeStamps[1];

                if (response instanceof GetTimesPlayedByUserQuery) {
                    getTimesPlayedByUserTurnaround += turnaround;
                    getTimesPlayedByUserExecution += execution;
                    getTimesPlayedByUserWaiting += waiting;
                } else if (response instanceof GetTimesPlayedQuery) {
                    getTimesPlayedTurnaround += turnaround;
                    getTimesPlayedExecution += execution;
                    getTimesPlayedWaiting += waiting;
                } else if (response instanceof GetTopArtistsByUserGenreQuery) {
                    getTopArtistsByUserGenreTurnaround += turnaround;
                    getTopArtistsByUserGenreExecution += execution;
                    getTopArtistsByUserGenreWaiting += waiting;
                } else if (response instanceof GetTopThreeMusicByUserQuery) {
                    getTopThreeMusicByUserTurnaround += turnaround;
                    getTopThreeMusicByUserExecution += execution;
                    getTopThreeMusicByUserWaiting += waiting;
                }
            }

            // Write the average times to file
            getTimesPlayedByUserTurnaround /= sentQueries;
            getTimesPlayedByUserExecution /= sentQueries;
            getTimesPlayedByUserWaiting /= sentQueries;

            getTimesPlayedTurnaround /= sentQueries;
            getTimesPlayedExecution /= sentQueries;
            getTimesPlayedWaiting /= sentQueries;

            getTopArtistsByUserGenreTurnaround /= sentQueries;
            getTopArtistsByUserGenreExecution /= sentQueries;
            getTopArtistsByUserGenreWaiting /= sentQueries;

            getTopThreeMusicByUserTurnaround /= sentQueries;
            getTopThreeMusicByUserExecution /= sentQueries;
            getTopThreeMusicByUserWaiting /= sentQueries;

            writer.write("\nAverage turnaround time for getTimesPlayedByUser queries: " + getTimesPlayedByUserTurnaround + "ms\n");
            writer.write("Average execution time for getTimesPlayedByUser queries: " + getTimesPlayedByUserExecution + "ms\n");
            writer.write("Average waiting time for getTimesPlayedByUser queries: " + getTimesPlayedByUserWaiting + "ms\n\n");

            writer.write("Average turnaround time for getTimesPlayed queries: " + getTimesPlayedTurnaround + "ms\n");
            writer.write("Average execution time for getTimesPlayed queries: " + getTimesPlayedExecution + "ms\n");
            writer.write("Average waiting time for getTimesPlayed queries: " + getTimesPlayedWaiting + "ms\n\n");

            writer.write("Average turnaround time for getTopArtistsByUserGenre queries: " + getTopArtistsByUserGenreTurnaround + "ms\n");
            writer.write("Average execution time for getTopArtistsByUserGenre queries: " + getTopArtistsByUserGenreExecution + "ms\n");
            writer.write("Average waiting time for getTopArtistsByUserGenre queries: " + getTopArtistsByUserGenreWaiting + "ms\n\n");

            writer.write("Average turnaround time for getTopThreeMusicByUser queries: " + getTopThreeMusicByUserTurnaround + "ms\n");
            writer.write("Average execution time for getTopThreeMusicByUser queries: " + getTopThreeMusicByUserExecution + "ms\n");
            writer.write("Average waiting time for getTopThreeMusicByUser queries: " + getTopThreeMusicByUserWaiting + "ms\n");

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("All query responses have been written to file.");
    }

    /**
     * Prompts the proxy-server to assign the client a server, then lookups the server address returned
     * from the proxy-server.
     *
     * @param zone: the zone in which the client is in.
     */
    private void getServerAssignment(int zone) {
        try {
            // Ask the proxy-server for a server address
            ServerAddress response = proxyServer.getServerAssignment(zone);

            // Lookup the returned server address
            server = (ServerInterface) registry.lookup(response.address);
        } catch (Exception e) {
            System.out.println("\nError:\n" + e);
            System.out.println("\nSomething went wrong when trying to get server assignment in client_" + clientNumber + ".");
            System.exit(1);
        }
    }
}
