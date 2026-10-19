PAPER FLIGHT — JAVA-INTEGRATED PROJECT

WHAT'S INCLUDED
- paper-plane-gust-challenge.html: the playable canvas game (HTML, CSS and JavaScript).
- PaperFlightEngine.java: Java reference engine with movement, wind influence, hostile projectiles, lives, collision detection and survival scoring.
- PaperFlightServer.java: local Java HTTP server that hosts the game and provides the score API.

WHAT IS ACTUALLY INTEGRATED?
When the game is opened from http://localhost:8787, the browser requests /api/score from PaperFlightServer about four times per second while playing. The server calls PaperFlightEngine.scoreForElapsed(...) to calculate survival-only score (10 points per second), then returns the result as JSON. The game displays the score returned by Java. Final survival score is also requested from Java when a run ends.

The browser still handles canvas graphics, keyboard/mouse input, live movement, wind visuals, incoming projectile animation, collisions and sound effects. The Java bridge is real and active during gameplay, rather than only showing a code sample. If the HTML is opened directly from Finder, the browser uses its local JavaScript scoring fallback instead.

HOW TO RUN ON macOS
1. Install a JDK (JDK 11 or newer; JDK 21 is recommended).
2. Extract this ZIP into a folder.
3. Open Terminal and change to the extracted folder. Tip: type `cd ` (include the space), drag the folder into Terminal, and press Return.
4. Compile both Java files:
   javac PaperFlightEngine.java PaperFlightServer.java
5. Start the server:
   java PaperFlightServer
6. Leave that Terminal window open and open this URL in your browser:
   http://localhost:8787
7. Check the badge near the score. It should read JAVA ENGINE CONNECTED.
8. To stop the server, go back to Terminal and press Ctrl+C.

PLAY CONTROLS
- WASD or arrow keys: fly
- Enter or click: start
- R: restart

OPTIONAL CONSOLE DEMO
You can also compile and run the Java reference simulation without hosting the browser game:
   javac PaperFlightEngine.java
   java PaperFlightEngine

NOTES
- The Java server binds to 127.0.0.1 (your own Mac) on port 8787; it is not exposed as a public server.
- If port 8787 is already in use, close the conflicting app or change PORT in PaperFlightServer.java and recompile.
