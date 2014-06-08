package org.example;

import java.io.IOException;
import java.io.Reader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SpamClassifierServlet extends HttpServlet {

    private SpamClassifier sc;

    public void init() {
        sc = new SpamClassifier();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Reader reader = req.getReader();
        try {

            long t0 = System.currentTimeMillis();
            String category = sc.classify(reader);
            long t1 = System.currentTimeMillis();

            resp.getWriter().print(String.format("{\"category\":\"%s\", \"time\": %d}", category, t1-t0));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
