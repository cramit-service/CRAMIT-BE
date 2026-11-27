CREATE UNIQUE INDEX uq_member_nickname_active ON member (nickname) WHERE is_deleted = FALSE;
