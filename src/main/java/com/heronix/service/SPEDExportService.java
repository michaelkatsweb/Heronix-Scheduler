package com.heronix.service;

import com.heronix.model.domain.IEP;
import com.heronix.model.domain.IEPService;
import com.heronix.model.domain.PullOutSchedule;
import com.heronix.model.domain.Teacher;

import java.io.File;
import java.util.List;

/**
 * SPED Export Service Interface
 *
 * Provides export functionality for SPED-related data including:
 * - IEP reports and compliance summaries
 * - Pull-out schedule exports
 * - Provider workload reports
 * - Service delivery reports
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8D - November 21, 2025
 */
public interface SPEDExportService {

    /**
     * Export SPED compliance report to PDF
     *
     * @return File object pointing to the generated PDF
     * @throws Exception if export fails
     */
    File exportComplianceReportPdf() throws Exception;

    /**
     * Export SPED compliance report to Excel
     *
     * @return File object pointing to the generated Excel file
     * @throws Exception if export fails
     */
    File exportComplianceReportExcel() throws Exception;

    /**
     * Export pull-out schedules to PDF
     *
     * @param schedules List of schedules to export
     * @return File object pointing to the generated PDF
     * @throws Exception if export fails
     */
    File exportPullOutSchedulesPdf(List<PullOutSchedule> schedules) throws Exception;

    /**
     * Export pull-out schedules to Excel
     *
     * @param schedules List of schedules to export
     * @return File object pointing to the generated Excel file
     * @throws Exception if export fails
     */
    File exportPullOutSchedulesExcel(List<PullOutSchedule> schedules) throws Exception;

    /**
     * Export pull-out schedules to CSV
     *
     * @param schedules List of schedules to export
     * @return File object pointing to the generated CSV file
     * @throws Exception if export fails
     */
    File exportPullOutSchedulesCsv(List<PullOutSchedule> schedules) throws Exception;

    /**
     * Export provider workload report to PDF
     *
     * @param providers List of providers
     * @return File object pointing to the generated PDF
     * @throws Exception if export fails
     */
    File exportProviderWorkloadPdf(List<Teacher> providers) throws Exception;

    /**
     * Export provider workload report to Excel
     *
     * @param providers List of providers
     * @return File object pointing to the generated Excel file
     * @throws Exception if export fails
     */
    File exportProviderWorkloadExcel(List<Teacher> providers) throws Exception;

    /**
     * Export IEP summary report to PDF
     *
     * @param iep IEP to export
     * @return File object pointing to the generated PDF
     * @throws Exception if export fails
     */
    File exportIepSummaryPdf(IEP iep) throws Exception;

    /**
     * Export all active IEPs summary to Excel
     *
     * @param ieps List of IEPs
     * @return File object pointing to the generated Excel file
     * @throws Exception if export fails
     */
    File exportIepsSummaryExcel(List<IEP> ieps) throws Exception;

    /**
     * Export service delivery report to PDF
     *
     * @param services List of services
     * @return File object pointing to the generated PDF
     * @throws Exception if export fails
     */
    File exportServiceDeliveryPdf(List<IEPService> services) throws Exception;

    /**
     * Export service delivery report to Excel
     *
     * @param services List of services
     * @return File object pointing to the generated Excel file
     * @throws Exception if export fails
     */
    File exportServiceDeliveryExcel(List<IEPService> services) throws Exception;

    /**
     * Get the exports directory for SPED reports
     *
     * @return File object representing the exports directory
     */
    File getExportsDirectory();
}
