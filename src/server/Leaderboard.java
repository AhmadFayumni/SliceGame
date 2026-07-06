/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Leaderboard {
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();

    public void updateScore(String username, int points) {
        scores.merge(username, points, Integer::sum);
    }

    public String getTopScores() {
        if (scores.isEmpty()) return "";
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
    }
}