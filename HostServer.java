/* 
	1. Jeff Wiand / 3-1-19
	2. Java 1.8
	3. Compilation Instructions:
    	> javac HostServer.java

	4. Run Instructions
   	 	> java HostServer
   	 	> open browser (i used google chrome) and type localhost:1565 to run program
   	 	> To increase the state (incrementer) of your session. Type in a name and hit submit
   	 	> To move that state to a new port. Type in migrate
   	 	> To launch a second state, open a new browser at localhost:1565 and repeat above steps

   	List of files needed for running the program
    	- HostServer.java

	5. My Notes
    	-
		
*/

import java.io.BufferedReader;                      //Pull in Input, Output libraries
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;                       //Pull in networking libraries
import java.net.Socket;


class AgentWorker extends Thread {                      // class definition of AgentWorker. Spawns from AgentListener run() method
	
	Socket sock;                                        // local definition of sock of tyep Socket
	agentHolder parentAgentHolder;                      // local definition of parentAgentHolder of type agentHolder. Holds state from agentHolder passed in through Constructor below
	int localPort;                                      // local definition of localPort of type int. Maintains Port for this instance
	

	AgentWorker (Socket s, int prt, agentHolder ah) {           //AgentWorker constructor: takes in Socket (s) , int port (prt), and state from parent agent (object agentHolder ah)
		sock = s;
		localPort = prt;
		parentAgentHolder = ah;
	}
	public void run() {                                         // definition of run() method that will execute when AgentWorker is spawned

		PrintStream out = null;                                 // local method initialization of PrintStream and BufferedReader to handle input/output
		BufferedReader in = null;

		String NewHost = "localhost";                           // String definition that holds the host. Can only be localhost in this version of the code
		int NewHostMainPort = 1565;		                        // sets the port back to 1565 again for localhost
		String buf = "";                                        // string that holds our response from the HostServer
		int newPort;                                            // local method definition of the port we will migrate to
		Socket clientSock;                                      // local definition of clientSock of type Socket
		BufferedReader fromHostServer;                          // local definition of fromHostServer of type BufferedReader (input)
		PrintStream toHostServer;                               // local definition of toHostServer of type PrintStream (output)
		
		try {                                                                                       //enter try block
		    out = new PrintStream(sock.getOutputStream());                                          //bind the Socket output to the PrintStream object   (setup an output avenue)
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));                  //bind the socket input to the BufferedReader object (get our input)

			String inLine = in.readLine();                                                          // set the request input to inLine String
			StringBuilder htmlString = new StringBuilder();                                         // build the html String with a StringBuilder object

			System.out.println();
			System.out.println("Request line: " + inLine);                                          // print the request to the console
			
			if(inLine.indexOf("migrate") > -1) {                                                    // if the input says "migrate", create a new connection at the same port 1565

				clientSock = new Socket(NewHost, NewHostMainPort);                                                      //create the socket connection
				fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));

				toHostServer = new PrintStream(clientSock.getOutputStream());                                           // set our output path to the new NewHost at NewPort
				toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]");     // print to the console the migrated agent state
				toHostServer.flush();                                                                                   // clear the out buffer

				for(;;) {
					buf = fromHostServer.readLine();                                                                    //continuously check if the NewPort is valid then exit
					if(buf.indexOf("[Port=") > -1) {
						break;
					}
				}

				String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) );                     //string hacking to obtain the Port number
				newPort = Integer.parseInt(tempbuf);                                                                                        //parse the value of the string into an Integer
				System.out.println("newPort is: " + newPort);                                                                               //print the new port to the console

				htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));                                                  //build the htmlString initialized above. Add newPort, NewHost, and input
				htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n");
				htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");       //print the new port and the source / location to the console
				htmlString.append(AgentListener.sendHTMLsubmit());                                                                          //add the necessary html submit lines

				System.out.println("Killing parent listening loop.");                                                   // tell the console that the parent is being killed off
				ServerSocket ss = parentAgentHolder.sock;                                                               // reach back and grab the parent sock and set it to new ServerSocket ss
				ss.close();                                                                                             // close this socket connection only
				
				
			} else if(inLine.indexOf("person") > -1) {                                                                  // checks if the input isn't "migrate" or "favicon.ico" request
				parentAgentHolder.agentState++;                                                                         // increment the state of person variable
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));                                        //format and send the new state back as html to browser
				htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
				htmlString.append(AgentListener.sendHTMLsubmit());

			} else {
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));                     // conditional to catch "favicon.ico" request and tell the user it's invalid
				htmlString.append("You have not entered a valid request!\n");
				htmlString.append(AgentListener.sendHTMLsubmit());		
				
		
			}
			AgentListener.sendHTMLtoStream(htmlString.toString(), out);             // call the sendHTMLtoStream method on AgentListener to output the formattted html response

			sock.close();                                                           // close the current connection only
			
		} catch (IOException ioe) {                                                 // catch and print any Input-Output exceptions here
			System.out.println(ioe);
		}
	}
	
}

class agentHolder {                                     //class definition of agentHolder.  This object will hold the sock of type ServerSocket and agentState of type int
	ServerSocket sock;
	int agentState;

	agentHolder(ServerSocket s) { sock = s;}            // constructor for agentHolder that accepts ServerSocket as input and sets to sock
}

class AgentListener extends Thread {                    // class definition of AgentListener. Spawns from HostServer main()
	Socket sock;                                        // local definition of sock of type Socket
	int localPort;                                      // local definition of localPort of type int

	AgentListener(Socket As, int prt) {                 // constructor for AgentListener class. Takes in Socket and int, sets as local sock of type Socket and local "localPort" of type int
		sock = As;
		localPort = prt;
	}

	int agentState = 0;                                 // set the intiial state for this agent definition to zero

	public void run() {                                 // local run() method that is executed when AgentWorker is spawned in HostServer
		BufferedReader in = null;                       // set input and output objects to null initially
		PrintStream out = null;
		String NewHost = "localhost";                           //use only our localhost
		System.out.println("In AgentListener Thread");		    //print we are in the AgentListener to the console
		try {                                                                               //enter try block
			String buf;                                                                     // string that holds the input
			out = new PrintStream(sock.getOutputStream());                                  // bind output to the socket outputStream
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));         // bind input to the socket inputStream
			

			buf = in.readLine();                                                            // read the input, set to String buf
			

			if(buf != null && buf.indexOf("[State=") > -1) {                                // check that there is input to be read and a state variable exists
				String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State=")));         //pull out the state substrings
				agentState = Integer.parseInt(tempbuf);                                                                         //set the string representation of the state variable to an Integer
				System.out.println("agentState is: " + agentState);                                                             // print this agentState to the console
					
			}
			
			System.out.println(buf);                                                                                    // print the full input line to the console

			StringBuilder htmlResponse = new StringBuilder();                                                           // instantitate a new Stringbuilder object to build our htmlResponse

			htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));                                               //set the html header
			htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");                         //add the text to be printed on the browser
			htmlResponse.append("[Port="+localPort+"]<br/>\n");
			htmlResponse.append(sendHTMLsubmit());                                                                      //add the submit text for the htmlResponse using the sendHTMLsubmit() helper method

			sendHTMLtoStream(htmlResponse.toString(), out);                                                             //Add html headers and send the response to the browser


			ServerSocket servsock = new ServerSocket(localPort,2);                                              //create new connection at the port set in the constructor
			agentHolder agenthold = new agentHolder(servsock);                                                          //create new agentHolder object to hold this Socket connection
			agenthold.agentState = agentState;                                                                          //add the agentState to this newly created object
			

			while(true) {                                                                                               //continuously look for server connections here
				sock = servsock.accept();
				System.out.println("Got a connection to agent at port " + localPort);                                   // print to the console when connected
				new AgentWorker(sock, localPort, agenthold).start();                                                    // spawn an AgentWorker and pass in the conneciton, port and agentHolder object
			}
		
		} catch(IOException ioe) {                                                                                          // catch and print input+output exceptions here and print errors to the console
			System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
			System.out.println(ioe);
		}
	}

	static String sendHTMLheader(int localPort, String NewHost, String inLine) {                                        // helper method to prepare+send HTMLHeader only
		
		StringBuilder htmlString = new StringBuilder();

		htmlString.append("<html><head> </head><body>\n");
		htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");            //display the ports and "localhost" for the current submission
		htmlString.append("<h3>You sent: "+ inLine + "</h3>");
		htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");              //tell the next form that it will go to "localhost" at whatever port is fed in from localPort
		htmlString.append("Enter text or <i>migrate</i>:");
		htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");
		
		return htmlString.toString();
	}

	static String sendHTMLsubmit() {                                                                    //adds this input submit HTML line to a response when called
		return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
	}

	static void sendHTMLtoStream(String html, PrintStream out) {                                //adds the necessary headers to be sent to the browser for a HTML response
		
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + html.length());
		out.println("Content-Type: text/html");
		out.println("");		
		out.println(html);
	}
	
}

public class HostServer {                                                                                   //class definition of HostServer: contains the main function of the file

	public static int NextPort = 3000;                                                                      // define where the Port base will be.  First port is 1 + 3000 = 3001
	
	public static void main(String[] a) throws IOException {                                                // main function definition
		int q_len = 6;                                                                                      // num of requests to be queued
		int port = 1565;                                                                                    // connect at port 1565
		Socket sock;
		
		ServerSocket servsock = new ServerSocket(port, q_len);                                              //create server socket connection at port 1565
		System.out.println("Jeff Wiand's DIA Master receiver started at port 1565.");                       // print to the console where we are connecting
		System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:1565\"\n");

		while(true) {                                                                                       // continuously listen for requests from browser here
			NextPort = NextPort + 1;                                                                        // start first port at 3001. Add 1 to each port when migrated or new browser instance is launched
			sock = servsock.accept();                                                                       // connect to the socket
			System.out.println("Starting AgentListener at port " + NextPort);                               // tell the console where we are connected
			new AgentListener(sock, NextPort).start();                                                      // spawn an AgentListener and pass it the socket and port. launch the AgentListener run() method internally
		}
		
	}
}