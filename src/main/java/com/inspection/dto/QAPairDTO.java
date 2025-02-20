package com.inspection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class QAPairDTO {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("index")
    private Integer index;
    
    @JsonProperty("question")
    private String question;
    
    @JsonProperty("answer")
    private String answer;
} 