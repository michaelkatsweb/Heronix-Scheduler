package com.heronix.prototypes;

/*
===========================================================================================
AdaptiveScheduler.java â€” Version 1.1
===========================================================================================

CORE IDEA:
-----------
This application implements a hybrid scheduling engine that combines constraint solving,
adaptive clustering, and AI-powered reasoning. It is designed to tackle complex scheduling
problems such as school master schedules, logistics routing, and resource allocation.

INTENT:
--------
To create a modular, production-ready scheduling system that:
- Uses OptaPlanner-style constraint solving for hard/soft scheduling rules
- Integrates NeuroCluster Streamer (NCS) for adaptive clustering and outlier detection
- Leverages a local FastAPI microservice to run NCS in Python
- Optionally uses a local LLM (via Ollama) to explain scheduling decisions

ARCHITECTURE OVERVIEW:
-----------------------
Java Application:
  - Entity: Represents scheduling units (students, teachers, etc.)
  - Cluster: Groups entities based on NCS clustering
  - ConstraintSolver: Assigns time slots using basic rules (can be extended with OptaPlanner)
  - ReasoningAgent: Calls Ollama to explain scheduling logic
  - NCSClient: Sends feature vectors to FastAPI and receives cluster assignments

Python Microservice (FastAPI):
  - Wraps the full NCS algorithm
  - Accepts feature vectors via POST
  - Returns cluster ID, outlier status, and outlier score

LLM Plugin (Optional):
  - Uses Ollama to run models like Mistral or LLaMA locally
  - Generates human-readable explanations of scheduling strategies

DEPLOYMENT NOTES:
------------------
- FastAPI runs locally on port 8000 (see ncs_api.py)
- Ollama must be installed and running locally
- Java uses HttpClient and Jackson for API communication and JSON parsing
- This file is self-contained for prototyping and can be modularized for production

===========================================================================================
*/

import java.net.http.*;
import java.net.URI;
import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;

public class AdaptiveScheduler {

    // --- Entity ---
    static class Entity {
        int id;
        double[] features;

        Entity(int id, double[] features) {
            this.id = id;
            this.features = features;
        }
    }

    // --- Cluster ---
    static class Cluster {
        int clusterId;
        List<Entity> members = new ArrayList<>();
        double healthScore;

        Cluster(int clusterId) {
            this.clusterId = clusterId;
        }

        void addMember(Entity e) {
            members.add(e);
        }

        void computeHealthScore() {
            healthScore = 1.0; // Placeholder, can be enhanced with additional metrics
        }
    }

    // --- NCS Client (calls FastAPI) ---
    static class NCSClient {
        private static final ObjectMapper mapper = new ObjectMapper();

        public static NCSResult processPoint(double[] features) throws IOException, InterruptedException {
            StringBuilder json = new StringBuilder("{\"features\": [");
            for (int i = 0; i < features.length; i++) {
                json.append(features[i]);
                if (i < features.length - 1)
                    json.append(", ");
            }
            json.append("]}");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8000/process"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return mapper.readValue(response.body(), NCSResult.class);
        }

        static class NCSResult {
            public int cluster_id;
            public boolean is_outlier;
            public double outlier_score;
        }
    }

    // --- Constraint Solver (stubbed) ---
    static class ConstraintSolver {
        Map<Integer, Integer> solve(Cluster cluster) {
            Map<Integer, Integer> schedule = new HashMap<>();
            int slot = 1;
            for (Entity e : cluster.members) {
                schedule.put(e.id, slot++);
                if (slot > 8)
                    slot = 1;
            }
            return schedule;
        }
    }

    // --- Reasoning Agent (Ollama plugin) ---
    static class ReasoningAgent {
        String explain(Cluster cluster, Map<Integer, Integer> schedule) {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Cluster ID: ").append(cluster.clusterId).append("\n");
            prompt.append("Size: ").append(cluster.members.size()).append("\n");
            prompt.append("Scheduling Outcome: ").append(schedule.toString()).append("\n");
            prompt.append("Explain the strategy used and any trade-offs.\n");

            try {
                ProcessBuilder pb = new ProcessBuilder("ollama", "run", "mistral");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                writer.write(prompt.toString());
                writer.flush();
                writer.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString().trim();
            } catch (IOException e) {
                return "LLM explanation failed: " + e.getMessage();
            }
        }
    }

    // --- Main Engine ---
    public static void main(String[] args) throws IOException, InterruptedException {
        List<Entity> entities = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < 30; i++) {
            double[] features = new double[5];
            for (int j = 0; j < 5; j++)
                features[j] = rand.nextDouble();
            entities.add(new Entity(i, features));
        }

        // Cluster assignment via FastAPI
        Map<Integer, Cluster> clusterMap = new HashMap<>();
        for (Entity e : entities) {
            NCSClient.NCSResult result = NCSClient.processPoint(e.features);
            int cid = result.cluster_id;
            clusterMap.putIfAbsent(cid, new Cluster(cid));
            clusterMap.get(cid).addMember(e);
        }

        ConstraintSolver solver = new ConstraintSolver();
        ReasoningAgent reasoner = new ReasoningAgent();

        for (Cluster cluster : clusterMap.values()) {
            cluster.computeHealthScore();
            Map<Integer, Integer> schedule = solver.solve(cluster);
            String explanation = reasoner.explain(cluster, schedule);

            System.out.println("\nCluster " + cluster.clusterId + ":");
            System.out.println("â†’ Schedule: " + schedule);
            System.out.println("â†’ Explanation:\n" + explanation);
        }
    }
}