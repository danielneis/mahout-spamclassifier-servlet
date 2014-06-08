public class SpamClassifierServlet extends HttpServlet {

    private SpamClassifier sc;

    public void init() {
        try {
            sc = new SpamClassifier();
            sc.init(new File("bayes-model"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidDatastoreException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Reader reader = req.getReader();
        try {

            long t0 = System.currentTimeMillis();
            String category = sc.classify(reader);
            long t1 = System.currentTimeMillis();

            resp.getWriter().print(String.format("{\"category\":\"%s\", \"time\": %d}", category, t1-t0));

        } catch (InvalidDatastoreException e) {
            e.printStackTrace();
        }

    }

}

