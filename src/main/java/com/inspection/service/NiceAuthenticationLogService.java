package com.inspection.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inspection.entity.Contract;
import com.inspection.entity.ContractParticipant;
import com.inspection.entity.NiceAuthenticationLog;
import com.inspection.nice.dto.NiceCertificationResultDto;
import com.inspection.repository.NiceAuthenticationLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * NICE 본인인증 로그 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NiceAuthenticationLogService {
    
    private final NiceAuthenticationLogRepository authLogRepository;
    
    /**
     * NICE 본인인증 완료 후 로그를 저장합니다.
     * 
     * @param contract 관련 계약
     * @param participant 인증한 계약 참여자
     * @param niceResult NICE 인증 결과
     * @param ipAddress 클라이언트 IP 주소
     * @param userAgent 클라이언트 User-Agent
     * @param authPurpose 인증 목적 (예: CONTRACT_SIGN, DOCUMENT_VIEW)
     * @return 저장된 인증 로그
     */
    @Transactional
    public NiceAuthenticationLog saveAuthenticationLog(
            Contract contract,
            ContractParticipant participant,
            NiceCertificationResultDto niceResult,
            String ipAddress,
            String userAgent,
            String authPurpose) {
        
        log.info("NICE 인증 로그 저장 시작: contractId={}, participantId={}, requestNo={}",
                contract.getId(), participant.getId(), niceResult.getRequestNo());
        
        try {
            // NiceAuthenticationLog 생성
            NiceAuthenticationLog authLog = NiceAuthenticationLog.from(
                    contract,
                    participant,
                    niceResult,
                    ipAddress,
                    userAgent,
                    authPurpose
            );
            
            // 로그 저장
            NiceAuthenticationLog savedLog = authLogRepository.save(authLog);
            
            // 인증 성공인 경우 ContractParticipant 정보 업데이트
            if (savedLog.isAuthenticationSuccess()) {
                participant.updateAuthenticationInfo(niceResult.getCi());
                log.info("계약 참여자 인증 정보 업데이트 완료: participantId={}, ci={}",
                        participant.getId(), niceResult.getCi());
            }
            
            log.info("NICE 인증 로그 저장 완료: logId={}, success={}",
                    savedLog.getId(), savedLog.isAuthenticationSuccess());
            
            return savedLog;
            
        } catch (Exception e) {
            log.error("NICE 인증 로그 저장 중 오류 발생: contractId={}, participantId={}",
                    contract.getId(), participant.getId(), e);
            throw new RuntimeException("인증 로그 저장 실패", e);
        }
    }
    
    /**
     * 계약 참여자의 인증 이력을 조회합니다.
     * 
     * @param participant 계약 참여자
     * @return 인증 이력 목록 (최신순)
     */
    public List<NiceAuthenticationLog> getAuthenticationHistory(ContractParticipant participant) {
        return authLogRepository.findByParticipantOrderByCreatedAtDesc(participant);
    }
    
    /**
     * 계약 참여자의 마지막 성공 인증을 조회합니다.
     * 
     * @param participant 계약 참여자
     * @return 마지막 성공 인증 로그
     */
    public Optional<NiceAuthenticationLog> getLastSuccessfulAuth(ContractParticipant participant) {
        return authLogRepository.findLastSuccessfulAuthByParticipant(participant);
    }
    
    /**
     * 계약별 인증 이력을 조회합니다.
     * 
     * @param contract 계약
     * @return 인증 이력 목록 (최신순)
     */
    public List<NiceAuthenticationLog> getContractAuthHistory(Contract contract) {
        return authLogRepository.findByContractOrderByCreatedAtDesc(contract);
    }
    
    /**
     * CI로 동일인의 인증 이력을 조회합니다.
     * 
     * @param ci 개인식별코드
     * @return 성공 인증 이력 목록 (최신순)
     */
    public List<NiceAuthenticationLog> getAuthHistoryByCi(String ci) {
        return authLogRepository.findByCiAndResultCodeOrderByCreatedAtDesc(ci, "0000");
    }
    
    /**
     * 계약 참여자가 최근 N시간 내에 인증했는지 확인합니다.
     * 
     * @param contract 계약
     * @param participant 계약 참여자
     * @param hours 시간 (예: 24시간)
     * @return 최근 인증 여부
     */
    public boolean hasRecentAuth(Contract contract, ContractParticipant participant, int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        return authLogRepository.hasRecentSuccessfulAuth(contract, participant, cutoffTime);
    }
    
    /**
     * 계약 참여자의 총 성공 인증 횟수를 조회합니다.
     * 
     * @param participant 계약 참여자
     * @return 성공 인증 횟수
     */
    public long getSuccessfulAuthCount(ContractParticipant participant) {
        return authLogRepository.countSuccessfulAuthByParticipant(participant);
    }
} 