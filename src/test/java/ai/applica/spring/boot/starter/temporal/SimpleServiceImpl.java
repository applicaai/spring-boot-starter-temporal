package ai.applica.spring.boot.starter.temporal;

import org.springframework.stereotype.Service;

@Service
public class SimpleServiceImpl implements SimpleService{
    public String  say(String sth){
        System.out.println(sth);
        return sth;
    }
}
