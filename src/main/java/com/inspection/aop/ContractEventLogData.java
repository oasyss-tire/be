package com.inspection.aop;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 계약 이벤트 로그에 사용되는 데이터 클래스 모음
 * ContractEventLogAspect에서 사용하는 다양한 이벤트 데이터를 위한 클래스들
 */
public class ContractEventLogData {

    /**
     * 상태 변경 데이터
     */
    public static class StatusChangeData {
        private String statusCodeId;
        private String statusName;
        
        public StatusChangeData() {}
        
        public StatusChangeData(String statusCodeId, String statusName) {
            this.statusCodeId = statusCodeId;
            this.statusName = statusName;
        }
        
        public String getStatusCodeId() {
            return statusCodeId;
        }
        
        public void setStatusCodeId(String statusCodeId) {
            this.statusCodeId = statusCodeId;
        }
        
        public String getStatusName() {
            return statusName;
        }
        
        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }
    }
    
    /**
     * 거부 데이터
     */
    public static class RejectionData {
        private String reason;
        
        public RejectionData() {}
        
        public RejectionData(String reason) {
            this.reason = reason;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
    
    /**
     * 비활성화 데이터
     */
    public static class DeactivationData {
        private String reason;
        
        public DeactivationData() {}
        
        public DeactivationData(String reason) {
            this.reason = reason;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
    
    /**
     * 재서명 요청 데이터
     */
    public static class ResignRequestData {
        private String reason;
        private Long participantId;
        private String requestedBy;
        private LocalDateTime requestedAt;
        
        public ResignRequestData() {}
        
        public ResignRequestData(String reason, Long participantId) {
            this.reason = reason;
            this.participantId = participantId;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
        
        public String getRequestedBy() {
            return requestedBy;
        }
        
        public void setRequestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
        }
        
        public LocalDateTime getRequestedAt() {
            return requestedAt;
        }
        
        public void setRequestedAt(LocalDateTime requestedAt) {
            this.requestedAt = requestedAt;
        }
    }
    
    /**
     * 재서명 완료 데이터
     */
    public static class ResignCompleteData {
        private Long participantId;
        private String participantName;
        private LocalDateTime completedAt;
        private List<String> resignedDocumentIds = new ArrayList<>();
        
        public ResignCompleteData() {}
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
        
        public String getParticipantName() {
            return participantName;
        }
        
        public void setParticipantName(String participantName) {
            this.participantName = participantName;
        }
        
        public LocalDateTime getCompletedAt() {
            return completedAt;
        }
        
        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }
        
        public List<String> getResignedDocumentIds() {
            return resignedDocumentIds;
        }
        
        public void setResignedDocumentIds(List<String> resignedDocumentIds) {
            this.resignedDocumentIds = resignedDocumentIds;
        }
        
        public void addResignedDocumentId(String documentId) {
            if (documentId != null && !documentId.isEmpty()) {
                this.resignedDocumentIds.add(documentId);
            }
        }
    }
    
    /**
     * 재서명 승인 데이터
     */
    public static class ResignApprovalData {
        private Long participantId;
        private String participantName;
        private String approvedBy;
        private LocalDateTime approvedAt;
        
        public ResignApprovalData() {}
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
        
        public String getParticipantName() {
            return participantName;
        }
        
        public void setParticipantName(String participantName) {
            this.participantName = participantName;
        }
        
        public String getApprovedBy() {
            return approvedBy;
        }
        
        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }
        
        public LocalDateTime getApprovedAt() {
            return approvedAt;
        }
        
        public void setApprovedAt(LocalDateTime approvedAt) {
            this.approvedAt = approvedAt;
        }
    }
    
    /**
     * 계약 생성 데이터
     */
    public static class ContractCreationData {
        private String contractNumber;
        private String title;
        private String statusCode;
        private String documentId;
        private String participantName;
        private Long participantId;
        
        public String getContractNumber() {
            return contractNumber;
        }
        
        public void setContractNumber(String contractNumber) {
            this.contractNumber = contractNumber;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getStatusCode() {
            return statusCode;
        }
        
        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }
        
        public String getDocumentId() {
            return documentId;
        }
        
        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }
        
        public String getParticipantName() {
            return participantName;
        }
        
        public void setParticipantName(String participantName) {
            this.participantName = participantName;
        }
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
    }
    
    /**
     * 참여자 서명 완료 데이터
     */
    public static class SignCompleteData {
        private Long participantId;
        private String participantName;
        private LocalDateTime signedAt;
        private String signedPdfId;
        private Long templateId;
        private String templateName;
        private Long templateMappingId;
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
        
        public String getParticipantName() {
            return participantName;
        }
        
        public void setParticipantName(String participantName) {
            this.participantName = participantName;
        }
        
        public LocalDateTime getSignedAt() {
            return signedAt;
        }
        
        public void setSignedAt(LocalDateTime signedAt) {
            this.signedAt = signedAt;
        }
        
        public String getSignedPdfId() {
            return signedPdfId;
        }
        
        public void setSignedPdfId(String signedPdfId) {
            this.signedPdfId = signedPdfId;
        }
        
        public Long getTemplateId() {
            return templateId;
        }
        
        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }
        
        public String getTemplateName() {
            return templateName;
        }
        
        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }
        
        public Long getTemplateMappingId() {
            return templateMappingId;
        }
        
        public void setTemplateMappingId(Long templateMappingId) {
            this.templateMappingId = templateMappingId;
        }
    }
    
    /**
     * 참여자 거부 데이터
     */
    public static class ParticipantRejectionData {
        private Long participantId;
        private String participantName;
        private String reason;
        private String rejectedBy;
        private LocalDateTime rejectedAt;
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
        
        public String getParticipantName() {
            return participantName;
        }
        
        public void setParticipantName(String participantName) {
            this.participantName = participantName;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getRejectedBy() {
            return rejectedBy;
        }
        
        public void setRejectedBy(String rejectedBy) {
            this.rejectedBy = rejectedBy;
        }
        
        public LocalDateTime getRejectedAt() {
            return rejectedAt;
        }
        
        public void setRejectedAt(LocalDateTime rejectedAt) {
            this.rejectedAt = rejectedAt;
        }
    }
    
    /**
     * 참여자 승인 데이터
     */
    public static class ParticipantApprovalData {
        private Long participantId;
        private String participantName;
        private String comment;
        private String approvedBy;
        private LocalDateTime approvedAt;
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
        
        public String getParticipantName() {
            return participantName;
        }
        
        public void setParticipantName(String participantName) {
            this.participantName = participantName;
        }
        
        public String getComment() {
            return comment;
        }
        
        public void setComment(String comment) {
            this.comment = comment;
        }
        
        public String getApprovedBy() {
            return approvedBy;
        }
        
        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }
        
        public LocalDateTime getApprovedAt() {
            return approvedAt;
        }
        
        public void setApprovedAt(LocalDateTime approvedAt) {
            this.approvedAt = approvedAt;
        }
    }
    
    /**
     * 참여자 문서 업로드 데이터
     */
    public static class DocumentUploadData {
        private Long documentId;
        private Long participantId;
        private String participantName;
        private String documentCodeId;
        private String documentName;
        private String originalFileName;
        private LocalDateTime uploadedAt;
        
        public DocumentUploadData() {}
        
        public Long getDocumentId() {
            return documentId;
        }
        
        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }
        
        public Long getParticipantId() {
            return participantId;
        }
        
        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }
        
        public String getParticipantName() {
            return participantName;
        }
        
        public void setParticipantName(String participantName) {
            this.participantName = participantName;
        }
        
        public String getDocumentCodeId() {
            return documentCodeId;
        }
        
        public void setDocumentCodeId(String documentCodeId) {
            this.documentCodeId = documentCodeId;
        }
        
        public String getDocumentName() {
            return documentName;
        }
        
        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }
        
        public String getOriginalFileName() {
            return originalFileName;
        }
        
        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }
        
        public LocalDateTime getUploadedAt() {
            return uploadedAt;
        }
        
        public void setUploadedAt(LocalDateTime uploadedAt) {
            this.uploadedAt = uploadedAt;
        }
    }
} 