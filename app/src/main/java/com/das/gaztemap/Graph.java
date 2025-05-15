package com.das.gaztemap;

import com.google.android.gms.maps.model.LatLng;

import java.util.*;

public class Graph {
    private final Map<LatLng, List<Edge>> adjacencyList = new HashMap<>();

    // Clase interna para representar una arista
    private static class Edge {
        LatLng destination;
        double weight;

        Edge(LatLng destination, double weight) {
            this.destination = destination;
            this.weight = weight;
        }
    }

    // Método para agregar una arista al grafo
    public void addEdge(LatLng source, LatLng destination, double weight) {
        adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(new Edge(destination, weight));
        adjacencyList.computeIfAbsent(destination, k -> new ArrayList<>()).add(new Edge(source, weight)); // Grafo no dirigido
    }

    public Set<LatLng> getNodes() {
        return adjacencyList.keySet();
    }

    // Método para calcular la ruta más corta usando Dijkstra
    public List<LatLng> getShortestPath(LatLng start, LatLng end) {
        Map<LatLng, Double> distances = new HashMap<>();
        Map<LatLng, LatLng> previousNodes = new HashMap<>();
        PriorityQueue<LatLng> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (LatLng node : adjacencyList.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        priorityQueue.add(start);

        while (!priorityQueue.isEmpty()) {
            LatLng current = priorityQueue.poll();

            if (current.equals(end)) {
                break;
            }

            for (Edge edge : adjacencyList.getOrDefault(current, new ArrayList<>())) {
                double newDist = distances.get(current) + edge.weight;
                if (newDist < distances.get(edge.destination)) {
                    distances.put(edge.destination, newDist);
                    previousNodes.put(edge.destination, current);
                    priorityQueue.add(edge.destination);
                }
            }
        }

        // Reconstruir el camino más corto
        List<LatLng> path = new ArrayList<>();
        for (LatLng at = end; at != null; at = previousNodes.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);

        return path.isEmpty() || !path.get(0).equals(start) ? Collections.emptyList() : path;
    }
}