To Compile:

run this line in the terminal "./build.sh"

To run:

run this to start the Server in one terminal:

    cd target/
    java -cp "./lib/jsoup-1.18.3.jar:." search.IndexServer

run this some other terminals to start the Robots (yes, terminals, one for each Robot):

    cd target/
    java -cp "./lib/jsoup-1.18.3.jar:." search.Robot

run this in another terminal to start the Client:

    cd target/
    java -cp "./lib/jsoup-1.18.3.jar:." search.Client

