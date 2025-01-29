package markovic.ana;

public enum JobConfType {
    EQUAL("all_diff_40_40_40"),
    SMALL("all_diff_100_10_10"),
    LARGE("all_diff_10_10_100"),
    REPETITIVE_SMALL("80_percent_small_100_10_10"),
    REPETITIVE_LARGE("80_percent_large_10_10_100");

    private final String jobName;

    JobConfType(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}
