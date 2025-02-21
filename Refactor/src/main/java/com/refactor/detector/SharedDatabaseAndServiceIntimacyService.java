package com.refactor.detector;

import com.refactor.dto.RequestItem;
import com.refactor.context.SharedDatabaseContext;
import com.refactor.utils.FileFactory;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-25 10:43
 */
@Service
public class SharedDatabaseAndServiceIntimacyService {

    @Autowired
    public FileFactory fileFactory;

    /**
     * @Description: collect data of shareDatabase and serviceIntimacy
     * @Param: [request]
     * @return: com.example.smelldetection.base.context.SharedDatabaseContext
     */
    public SharedDatabaseContext getsharedDatabaseandServiceIntimacy(String path) throws IOException {
        String servicesDirectory = new File(path).getAbsolutePath();
        System.out.println("servicesDirectory" +servicesDirectory);
        if (fileFactory == null)
            System.out.println("------------NULL-------");
        List<String> applicationYamlOrProperties= fileFactory.getApplicationYamlOrPropertities(servicesDirectory);
        Yaml yaml = new Yaml();
        HashMap<String, ArrayList<String>> databaseMap = new HashMap<>();
        HashMap<String, ArrayList<String>> ServiceIntimacyMap = new HashMap<>();
        SharedDatabaseContext sharedDatabaseContext = new SharedDatabaseContext();
        for(String app: applicationYamlOrProperties){
            String serviceName = "";
            System.out.println("app"+app);
            if(app.endsWith("yaml") || app.endsWith("yml")){
                Map map = yaml.load(new FileInputStream(app));
                Map m1 =(Map)map.get("spring");
                Map m2 = (Map)m1.get("application");
                if(m2 != null)
                    serviceName = (String)m2.get("name");
            }
            else{
                InputStream in = new BufferedInputStream(new FileInputStream(app));
                Properties p = new Properties();
                p.load(in);
                serviceName = (String)p.get("spring.application.name");
            }
            BufferedReader reader = new BufferedReader(new FileReader(app));
            String line = reader.readLine();
            String pattern = "mysql://";
            String target = "";
            while (line != null) {
                if (line.contains(pattern) && !line.contains("#")) {
                    int startIndex = line.indexOf(pattern) + 8;
                    int endIndex = line.indexOf("?");
                    if (line.contains("///")) {
                        startIndex = line.indexOf("///") + 3;
                        target = "localhost:3306/" + line.substring(startIndex, endIndex);
                    }
                    else if(line.contains("127.0.0.1")){
                        startIndex = line.indexOf("//")+2;
                        target = line.substring(startIndex, endIndex);
                        target = target.replace("localhost","127.0.0.1");
                    }
                    else {
                        target = line.substring(startIndex, endIndex);
                        System.out.println("TARGET" +target);
                    }
                    if (databaseMap.containsKey(target)) {
                        databaseMap.get(target).add(serviceName);
                    } else {
                        databaseMap.put(target, new ArrayList<>());
                        databaseMap.get(target).add(serviceName);
                    }
                    if (ServiceIntimacyMap.containsKey(serviceName)) {
                        ServiceIntimacyMap.get(serviceName).add(target);
                    } else {
                        ServiceIntimacyMap.put(serviceName, new ArrayList<>());
                        ServiceIntimacyMap.get(serviceName).add(target);
                    }
                }
                line = reader.readLine();
            }
        }
        sharedDatabaseContext.addServicecDatabasesMap(ServiceIntimacyMap);
        Set<String> keys = databaseMap.keySet();
        for(String key:keys){
            ArrayList<String> servicesList=databaseMap.get(key);
            if(servicesList.size()>1) {
                boolean shared = true;
                for (String service : servicesList) {
                    if (ServiceIntimacyMap.get(service).size() > 1) {
                        shared = false;
                        break;
                    }
                }
                if(shared){
                    sharedDatabaseContext.addSharedDatabase(key,servicesList);
                }
                else sharedDatabaseContext.addServiceIntimacy(key,servicesList);
            }
        }
        return sharedDatabaseContext;
    }
}