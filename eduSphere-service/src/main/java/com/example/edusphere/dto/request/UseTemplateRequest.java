package com.example.edusphere.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UseTemplateRequest {

    private List<VariableValue> variableValues;

    private List<String> recipientIds;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableValue {
        private String name;
        private String value;
    }
}