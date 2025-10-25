To Compile:

run this line in the terminal "./build.sh"

To run:

run this to start the Server in one terminal:

    cd target/
    java -cp "./lib/jsoup-1.18.3.jar:." search.IndexServer

run this to start the Gateway server (and consequentially the RobotWorkers):

    cd target/
    java -cp "./lib/jsoup-1.18.3.jar:." search.Gateway

run this in another terminal to start the Client (can run more than 1, each on their terminal):

    cd target/
    java -cp "./lib/jsoup-1.18.3.jar:." search.Client

