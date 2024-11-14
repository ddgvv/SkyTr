package com.es.transalte.skytranslate.service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataFrameFilter {

    public static List<Map<String, Object>> filterDataFrame(List<Map<String, Object>> df, Map<String, FilterCriteria> filterCriteria) {
        List<Map<String, Object>> filteredData = new ArrayList<>(df);

        for (Map.Entry<String, FilterCriteria> entry : filterCriteria.entrySet()) {
            String column = entry.getKey();
            FilterCriteria criteria = entry.getValue();

            // Apply filter based on criteria type
            switch (criteria.getType()) {
                case CATEGORY:
                    Set<Object> categoryValues = criteria.getCategoryValues();
                    filteredData = filteredData.stream()
                        .filter(row -> categoryValues.contains(row.get(column)))
                        .collect(Collectors.toList());
                    break;

                case NUMERIC:
                    double min = criteria.getMinValue();
                    double max = criteria.getMaxValue();
                    filteredData = filteredData.stream()
                        .filter(row -> {
                            Object value = row.get(column);
                            if (value instanceof Number) {
                                double numValue = ((Number) value).doubleValue();
                                return numValue >= min && numValue <= max;
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
                    break;

                case DATE:
                    Date startDate = criteria.getStartDate();
                    Date endDate = criteria.getEndDate();
                    filteredData = filteredData.stream()
                        .filter(row -> {
                            Object value = row.get(column);
                            if (value instanceof Date) {
                                Date dateValue = (Date) value;
                                return !dateValue.before(startDate) && !dateValue.after(endDate);
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
                    break;

                case TEXT:
                    String textPattern = criteria.getTextPattern();
                    filteredData = filteredData.stream()
                        .filter(row -> {
                            Object value = row.get(column);
                            return value != null && value.toString().contains(textPattern);
                        })
                        .collect(Collectors.toList());
                    break;
            }
        }

        return filteredData;
    }

    public static class FilterCriteria {
        private FilterType type;
        private Set<Object> categoryValues;
        private double minValue;
        private double maxValue;
        private Date startDate;
        private Date endDate;
        private String textPattern;

        // Getters, setters, and constructors
        public FilterType getType() { return type; }
        public void setType(FilterType type) { this.type = type; }

        public Set<Object> getCategoryValues() { return categoryValues; }
        public void setCategoryValues(Set<Object> categoryValues) { this.categoryValues = categoryValues; }

        public double getMinValue() { return minValue; }
        public void setMinValue(double minValue) { this.minValue = minValue; }
        public double getMaxValue() { return maxValue; }
        public void setMaxValue(double maxValue) { this.maxValue = maxValue; }

        public Date getStartDate() { return startDate; }
        public void setStartDate(Date startDate) { this.startDate = startDate; }
        public Date getEndDate() { return endDate; }
        public void setEndDate(Date endDate) { this.endDate = endDate; }

        public String getTextPattern() { return textPattern; }
        public void setTextPattern(String textPattern) { this.textPattern = textPattern; }
    }

    public enum FilterType {
        CATEGORY,
        NUMERIC,
        DATE,
        TEXT
    }
}
