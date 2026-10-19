# Publish Paper Flight online (Render)

This version is configured for a public Render web service. The Docker image compiles the Java engine and starts the Java server. The server listens on Render's `PORT` environment variable and binds to all network interfaces; these are required for the public service to receive traffic.

## Fastest path using the GitHub web interface

1. Sign in to GitHub and create a **public repository** named `paper-flight`.
2. Unzip this deploy package on your computer.
3. In the new GitHub repository, choose **Add file → Upload files** and upload the files and `.gitignore` from this folder. Upload the files inside the folder, not the outer ZIP archive. Ensure `Dockerfile` and `render.yaml` are at the repository root.
4. Commit the uploaded files to the repository's `main` branch.
5. Sign in to Render and select **New → Blueprint** (or create a new Web Service and connect the repository).
6. Select the `paper-flight` GitHub repository. Render reads `render.yaml`, builds the Dockerfile, and starts the Java web server.
7. When the deploy completes, open the public `https://<service-name>.onrender.com` URL that Render shows.

## Alternative: create a web service manually

- Runtime/environment: Docker
- Dockerfile path: `./Dockerfile`
- Health check path: `/api/status`
- Plan: Free (for demos)
- Ensure the service is allowed to use the `PORT` variable (the provided server does so automatically).

## Test the deployment

- Open the root URL to play the game.
- Open `/api/status` and verify it returns JSON with `"connected":true` and `"engine":"Java"`.
- The game's Java badge should show `JAVA ENGINE CONNECTED` once the status check succeeds.

## Important notes

- I cannot publish to a hosting account without its account authorization. This folder is deploy-ready, but it is not online until you complete the Render/GitHub steps and Render reports a successful deployment.
- Render's free web services can spin down after 15 minutes without inbound traffic; the next visit can take around a minute to wake up. This is a Render free-tier limitation.
- The best-score record is stored in each player's own browser, not in a shared leaderboard.
