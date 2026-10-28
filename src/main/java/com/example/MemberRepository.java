package com.example;

import java.util.Optional;

//No implementation exists - in tests we replace it with a mock.
public interface MemberRepository {

    Optional<Member> findById(String id);
}
