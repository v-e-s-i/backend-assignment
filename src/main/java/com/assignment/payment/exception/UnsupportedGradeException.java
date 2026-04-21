package com.assignment.payment.exception;

import com.assignment.payment.domain.member.MemberGrade;

public class UnsupportedGradeException extends RuntimeException {

    public UnsupportedGradeException(MemberGrade grade) {
        super("No discount policy found for grade: " + grade);
    }
}
