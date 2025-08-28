package com.example.community.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatsResponse {
    private int totalFriends;
    private int totalGroups;
    private int totalPosts;
    private int totalJobsApplied;
    private int challengesCompleted;
    private int totalPoints;

    public StatsResponse() {}

    public StatsResponse(int totalFriends, int totalGroups, int totalPosts,
                         int totalJobsApplied, int challengesCompleted, int totalPoints) {
        this.totalFriends = totalFriends;
        this.totalGroups = totalGroups;
        this.totalPosts = totalPosts;
        this.totalJobsApplied = totalJobsApplied;
        this.challengesCompleted = challengesCompleted;
        this.totalPoints = totalPoints;
    }
}