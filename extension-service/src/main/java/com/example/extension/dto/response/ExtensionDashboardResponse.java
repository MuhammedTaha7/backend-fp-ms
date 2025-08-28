package com.example.extension.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Main dashboard response for extension
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionDashboardResponse {
    private List<ExtensionItemResponse> items;
    private ExtensionStatsResponse stats;
}