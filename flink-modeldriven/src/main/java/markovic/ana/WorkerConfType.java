package markovic.ana;

public enum WorkerConfType {
    EQUAL("conf_all_equal"),
    ONE_FAST("conf_one_fast"),
    ONE_SLOW("conf_one_slow"),
    SLOW_FAST("conf_slow_fast");

    private final String confName;

    WorkerConfType(String confName) {
        this.confName = confName;
    }

    public String getConfName() {
        return confName;
    }
}
