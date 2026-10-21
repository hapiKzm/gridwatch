CREATE TABLE public.sent_notification (
                                          work_order_id bigint NOT NULL PRIMARY KEY,
                                          sent_at timestamp(6) with time zone NOT NULL
);
