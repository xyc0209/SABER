package com.refactor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
public class RequestItem {
    public String servicesPath;
    //can be replaced with annotation @AllArgsConstructor
    public RequestItem(String servicesPath){
        this.servicesPath = servicesPath;
    }


}
