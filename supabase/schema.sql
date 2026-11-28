-- ============================================================================
-- Teman Kontrol — Supabase PostgreSQL Schema, RLS Policies, Storage & Sync RPC
-- Project: temankontrol (https://hqtrpbagnosmrznjiiph.supabase.co)
-- Jalankan seluruh file ini sekali di Supabase Dashboard -> SQL Editor -> Run.
-- ============================================================================

-- 1. Tables ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.consultations (
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    id text NOT NULL,
    title text NOT NULL,
    appointment_at bigint NOT NULL,
    reason text NOT NULL,
    facility_name text,
    provider_name text,
    status text NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    deleted_at bigint,
    PRIMARY KEY (user_id, id)
);
CREATE INDEX IF NOT EXISTS idx_consultations_user_updated
    ON public.consultations (user_id, updated_at);

CREATE TABLE IF NOT EXISTS public.questions (
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    id text NOT NULL,
    consultation_id text NOT NULL,
    text text NOT NULL,
    is_priority boolean NOT NULL DEFAULT false,
    is_discussed boolean NOT NULL DEFAULT false,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    answer text,
    answer_updated_at bigint,
    deleted_at bigint,
    PRIMARY KEY (user_id, id)
);
CREATE INDEX IF NOT EXISTS idx_questions_user_updated
    ON public.questions (user_id, updated_at);

CREATE TABLE IF NOT EXISTS public.consultation_notes (
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    consultation_id text NOT NULL,
    summary text,
    provider_instruction text,
    additional_notes text,
    updated_at bigint NOT NULL,
    deleted_at bigint,
    PRIMARY KEY (user_id, consultation_id)
);
CREATE INDEX IF NOT EXISTS idx_notes_user_updated
    ON public.consultation_notes (user_id, updated_at);

CREATE TABLE IF NOT EXISTS public.follow_up_actions (
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    id text NOT NULL,
    consultation_id text NOT NULL,
    text text NOT NULL,
    due_date_epoch_day bigint,
    is_completed boolean NOT NULL DEFAULT false,
    completed_at bigint,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    deleted_at bigint,
    PRIMARY KEY (user_id, id)
);
CREATE INDEX IF NOT EXISTS idx_followups_user_updated
    ON public.follow_up_actions (user_id, updated_at);

CREATE TABLE IF NOT EXISTS public.attachments (
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    id text NOT NULL,
    consultation_id text NOT NULL,
    question_id text,
    target text NOT NULL,
    kind text NOT NULL,
    file_name text NOT NULL,
    mime_type text NOT NULL,
    size_bytes bigint NOT NULL,
    duration_ms bigint,
    caption text,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    deleted_at bigint,
    PRIMARY KEY (user_id, id)
);
CREATE INDEX IF NOT EXISTS idx_attachments_user_updated
    ON public.attachments (user_id, updated_at);

CREATE TABLE IF NOT EXISTS public.user_settings (
    user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    visit_reminder_enabled boolean NOT NULL,
    visit_reminder_hour integer NOT NULL,
    visit_30m_enabled boolean NOT NULL,
    visit_at_time_enabled boolean NOT NULL,
    custom_lead_minutes integer,
    alarm_sound_mode_enabled boolean NOT NULL,
    prepare_reminder_enabled boolean NOT NULL,
    followup_reminder_enabled boolean NOT NULL,
    followup_reminder_hour integer NOT NULL,
    backup_reminder_enabled boolean NOT NULL,
    show_title_on_lock_screen boolean NOT NULL,
    updated_at bigint NOT NULL
);

-- 2. Row Level Security (RLS) ------------------------------------------------

ALTER TABLE public.consultations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.consultation_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follow_up_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage own consultations" ON public.consultations;
CREATE POLICY "Users manage own consultations"
    ON public.consultations FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own questions" ON public.questions;
CREATE POLICY "Users manage own questions"
    ON public.questions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own notes" ON public.consultation_notes;
CREATE POLICY "Users manage own notes"
    ON public.consultation_notes FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own follow_ups" ON public.follow_up_actions;
CREATE POLICY "Users manage own follow_ups"
    ON public.follow_up_actions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own attachments" ON public.attachments;
CREATE POLICY "Users manage own attachments"
    ON public.attachments FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own user_settings" ON public.user_settings;
CREATE POLICY "Users manage own user_settings"
    ON public.user_settings FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. Private Storage Bucket & Policies ---------------------------------------

INSERT INTO storage.buckets (id, name, public, file_size_limit)
VALUES ('attachments', 'attachments', false, 15728640) -- 15 MB per file
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = 15728640;

DROP POLICY IF EXISTS "Users read own attachment files" ON storage.objects;
CREATE POLICY "Users read own attachment files"
    ON storage.objects FOR SELECT
    USING (
        bucket_id = 'attachments'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Users upload own attachment files" ON storage.objects;
CREATE POLICY "Users upload own attachment files"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'attachments'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Users update own attachment files" ON storage.objects;
CREATE POLICY "Users update own attachment files"
    ON storage.objects FOR UPDATE
    USING (
        bucket_id = 'attachments'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    )
    WITH CHECK (
        bucket_id = 'attachments'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Users delete own attachment files" ON storage.objects;
CREATE POLICY "Users delete own attachment files"
    ON storage.objects FOR DELETE
    USING (
        bucket_id = 'attachments'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- 4. Atomic LWW Sync RPC Functions -------------------------------------------

CREATE OR REPLACE FUNCTION public.sync_push(payload jsonb)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_user_id uuid := auth.uid();
    v_accepted integer := 0;
    v_skipped integer := 0;
    v_server_now bigint := (extract(epoch FROM clock_timestamp()) * 1000)::bigint;
    v_existing_updated bigint;
    item jsonb;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'UNAUTHORIZED: Autentikasi diperlukan.' USING ERRCODE = '28000';
    END IF;

    -- 1. Consultations
    FOR item IN SELECT * FROM jsonb_array_elements(COALESCE(payload->'consultations', '[]'::jsonb))
    LOOP
        SELECT updated_at INTO v_existing_updated
        FROM public.consultations
        WHERE user_id = v_user_id AND id = item->>'id';

        IF v_existing_updated IS NOT NULL AND v_existing_updated > (item->>'updatedAtMillis')::bigint THEN
            v_skipped := v_skipped + 1;
        ELSE
            INSERT INTO public.consultations (
                user_id, id, title, appointment_at, reason,
                facility_name, provider_name, status,
                created_at, updated_at, deleted_at
            ) VALUES (
                v_user_id,
                item->>'id',
                item->>'title',
                (item->>'appointmentAtMillis')::bigint,
                item->>'reason',
                item->>'facilityName',
                item->>'providerName',
                item->>'status',
                (item->>'createdAtMillis')::bigint,
                (item->>'updatedAtMillis')::bigint,
                NULLIF(item->>'deletedAtMillis', '')::bigint
            )
            ON CONFLICT (user_id, id) DO UPDATE SET
                title = EXCLUDED.title,
                appointment_at = EXCLUDED.appointment_at,
                reason = EXCLUDED.reason,
                facility_name = EXCLUDED.facility_name,
                provider_name = EXCLUDED.provider_name,
                status = EXCLUDED.status,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at;
            v_accepted := v_accepted + 1;
        END IF;
    END LOOP;

    -- 2. Questions
    FOR item IN SELECT * FROM jsonb_array_elements(COALESCE(payload->'questions', '[]'::jsonb))
    LOOP
        SELECT updated_at INTO v_existing_updated
        FROM public.questions
        WHERE user_id = v_user_id AND id = item->>'id';

        IF v_existing_updated IS NOT NULL AND v_existing_updated > (item->>'updatedAtMillis')::bigint THEN
            v_skipped := v_skipped + 1;
        ELSE
            INSERT INTO public.questions (
                user_id, id, consultation_id, text, is_priority, is_discussed,
                created_at, updated_at, answer, answer_updated_at, deleted_at
            ) VALUES (
                v_user_id,
                item->>'id',
                item->>'consultationId',
                item->>'text',
                COALESCE((item->>'isPriority')::boolean, false),
                COALESCE((item->>'isDiscussed')::boolean, false),
                (item->>'createdAtMillis')::bigint,
                (item->>'updatedAtMillis')::bigint,
                item->>'answer',
                NULLIF(item->>'answerUpdatedAtMillis', '')::bigint,
                NULLIF(item->>'deletedAtMillis', '')::bigint
            )
            ON CONFLICT (user_id, id) DO UPDATE SET
                text = EXCLUDED.text,
                is_priority = EXCLUDED.is_priority,
                is_discussed = EXCLUDED.is_discussed,
                updated_at = EXCLUDED.updated_at,
                answer = EXCLUDED.answer,
                answer_updated_at = EXCLUDED.answer_updated_at,
                deleted_at = EXCLUDED.deleted_at;
            v_accepted := v_accepted + 1;
        END IF;
    END LOOP;

    -- 3. Notes
    FOR item IN SELECT * FROM jsonb_array_elements(COALESCE(payload->'notes', '[]'::jsonb))
    LOOP
        SELECT updated_at INTO v_existing_updated
        FROM public.consultation_notes
        WHERE user_id = v_user_id AND consultation_id = item->>'consultationId';

        IF v_existing_updated IS NOT NULL AND v_existing_updated > (item->>'updatedAtMillis')::bigint THEN
            v_skipped := v_skipped + 1;
        ELSE
            INSERT INTO public.consultation_notes (
                user_id, consultation_id, summary, provider_instruction,
                additional_notes, updated_at, deleted_at
            ) VALUES (
                v_user_id,
                item->>'consultationId',
                item->>'summary',
                item->>'providerInstruction',
                item->>'additionalNotes',
                (item->>'updatedAtMillis')::bigint,
                NULLIF(item->>'deletedAtMillis', '')::bigint
            )
            ON CONFLICT (user_id, consultation_id) DO UPDATE SET
                summary = EXCLUDED.summary,
                provider_instruction = EXCLUDED.provider_instruction,
                additional_notes = EXCLUDED.additional_notes,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at;
            v_accepted := v_accepted + 1;
        END IF;
    END LOOP;

    -- 4. Follow-Up Actions
    FOR item IN SELECT * FROM jsonb_array_elements(COALESCE(payload->'followUps', '[]'::jsonb))
    LOOP
        SELECT updated_at INTO v_existing_updated
        FROM public.follow_up_actions
        WHERE user_id = v_user_id AND id = item->>'id';

        IF v_existing_updated IS NOT NULL AND v_existing_updated > (item->>'updatedAtMillis')::bigint THEN
            v_skipped := v_skipped + 1;
        ELSE
            INSERT INTO public.follow_up_actions (
                user_id, id, consultation_id, text, due_date_epoch_day,
                is_completed, completed_at, created_at, updated_at, deleted_at
            ) VALUES (
                v_user_id,
                item->>'id',
                item->>'consultationId',
                item->>'text',
                NULLIF(item->>'dueDateEpochDay', '')::bigint,
                COALESCE((item->>'isCompleted')::boolean, false),
                NULLIF(item->>'completedAtMillis', '')::bigint,
                (item->>'createdAtMillis')::bigint,
                (item->>'updatedAtMillis')::bigint,
                NULLIF(item->>'deletedAtMillis', '')::bigint
            )
            ON CONFLICT (user_id, id) DO UPDATE SET
                text = EXCLUDED.text,
                due_date_epoch_day = EXCLUDED.due_date_epoch_day,
                is_completed = EXCLUDED.is_completed,
                completed_at = EXCLUDED.completed_at,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at;
            v_accepted := v_accepted + 1;
        END IF;
    END LOOP;

    -- 5. Attachment Metadata
    FOR item IN SELECT * FROM jsonb_array_elements(COALESCE(payload->'attachments', '[]'::jsonb))
    LOOP
        SELECT updated_at INTO v_existing_updated
        FROM public.attachments
        WHERE user_id = v_user_id AND id = item->>'id';

        IF v_existing_updated IS NOT NULL AND v_existing_updated > (item->>'updatedAtMillis')::bigint THEN
            v_skipped := v_skipped + 1;
        ELSE
            INSERT INTO public.attachments (
                user_id, id, consultation_id, question_id, target, kind,
                file_name, mime_type, size_bytes, duration_ms, caption,
                created_at, updated_at, deleted_at
            ) VALUES (
                v_user_id,
                item->>'id',
                item->>'consultationId',
                item->>'questionId',
                item->>'target',
                item->>'kind',
                item->>'fileName',
                item->>'mimeType',
                (item->>'sizeBytes')::bigint,
                NULLIF(item->>'durationMs', '')::bigint,
                item->>'caption',
                (item->>'createdAtMillis')::bigint,
                (item->>'updatedAtMillis')::bigint,
                NULLIF(item->>'deletedAtMillis', '')::bigint
            )
            ON CONFLICT (user_id, id) DO UPDATE SET
                caption = EXCLUDED.caption,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at;
            v_accepted := v_accepted + 1;
        END IF;
    END LOOP;

    -- 6. Reminder Settings
    IF payload->'reminderSettings' IS NOT NULL AND payload->'reminderSettings' <> 'null'::jsonb THEN
        item := payload->'reminderSettings';
        SELECT updated_at INTO v_existing_updated
        FROM public.user_settings
        WHERE user_id = v_user_id;

        IF v_existing_updated IS NOT NULL AND v_existing_updated > (item->>'updatedAtMillis')::bigint THEN
            v_skipped := v_skipped + 1;
        ELSE
            INSERT INTO public.user_settings (
                user_id, visit_reminder_enabled, visit_reminder_hour,
                visit_30m_enabled, visit_at_time_enabled, custom_lead_minutes,
                alarm_sound_mode_enabled, prepare_reminder_enabled,
                followup_reminder_enabled, followup_reminder_hour,
                backup_reminder_enabled, show_title_on_lock_screen, updated_at
            ) VALUES (
                v_user_id,
                (item->>'visitReminderEnabled')::boolean,
                (item->>'visitReminderHour')::integer,
                (item->>'visitThirtyMinBeforeEnabled')::boolean,
                (item->>'visitAtTimeEnabled')::boolean,
                NULLIF(item->>'customLeadMinutes', '')::integer,
                (item->>'alarmSoundModeEnabled')::boolean,
                (item->>'prepareReminderEnabled')::boolean,
                (item->>'followUpReminderEnabled')::boolean,
                (item->>'followUpReminderHour')::integer,
                (item->>'backupReminderEnabled')::boolean,
                (item->>'showTitleOnLockScreen')::boolean,
                (item->>'updatedAtMillis')::bigint
            )
            ON CONFLICT (user_id) DO UPDATE SET
                visit_reminder_enabled = EXCLUDED.visit_reminder_enabled,
                visit_reminder_hour = EXCLUDED.visit_reminder_hour,
                visit_30m_enabled = EXCLUDED.visit_30m_enabled,
                visit_at_time_enabled = EXCLUDED.visit_at_time_enabled,
                custom_lead_minutes = EXCLUDED.custom_lead_minutes,
                alarm_sound_mode_enabled = EXCLUDED.alarm_sound_mode_enabled,
                prepare_reminder_enabled = EXCLUDED.prepare_reminder_enabled,
                followup_reminder_enabled = EXCLUDED.followup_reminder_enabled,
                followup_reminder_hour = EXCLUDED.followup_reminder_hour,
                backup_reminder_enabled = EXCLUDED.backup_reminder_enabled,
                show_title_on_lock_screen = EXCLUDED.show_title_on_lock_screen,
                updated_at = EXCLUDED.updated_at;
            v_accepted := v_accepted + 1;
        END IF;
    END IF;

    RETURN jsonb_build_object(
        'acceptedCount', v_accepted,
        'skippedConflictsCount', v_skipped,
        'serverTimestampMillis', v_server_now
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.sync_pull(since_millis bigint DEFAULT 0)
RETURNS jsonb
LANGUAGE plpgsql
STABLE
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    v_user_id uuid := auth.uid();
    v_server_now bigint := (extract(epoch FROM clock_timestamp()) * 1000)::bigint;
    v_consultations jsonb;
    v_questions jsonb;
    v_notes jsonb;
    v_follow_ups jsonb;
    v_attachments jsonb;
    v_settings jsonb;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'UNAUTHORIZED: Autentikasi diperlukan.' USING ERRCODE = '28000';
    END IF;

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'id', c.id,
        'title', c.title,
        'appointmentAtMillis', c.appointment_at,
        'reason', c.reason,
        'facilityName', c.facility_name,
        'providerName', c.provider_name,
        'status', c.status,
        'createdAtMillis', c.created_at,
        'updatedAtMillis', c.updated_at,
        'deletedAtMillis', c.deleted_at
    )), '[]'::jsonb)
    INTO v_consultations
    FROM public.consultations c
    WHERE c.user_id = v_user_id
      AND (c.updated_at >= since_millis OR (c.deleted_at IS NOT NULL AND c.deleted_at >= since_millis));

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'id', q.id,
        'consultationId', q.consultation_id,
        'text', q.text,
        'isPriority', q.is_priority,
        'isDiscussed', q.is_discussed,
        'createdAtMillis', q.created_at,
        'updatedAtMillis', q.updated_at,
        'answer', q.answer,
        'answerUpdatedAtMillis', q.answer_updated_at,
        'deletedAtMillis', q.deleted_at
    )), '[]'::jsonb)
    INTO v_questions
    FROM public.questions q
    WHERE q.user_id = v_user_id
      AND (q.updated_at >= since_millis OR (q.deleted_at IS NOT NULL AND q.deleted_at >= since_millis));

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'consultationId', n.consultation_id,
        'summary', n.summary,
        'providerInstruction', n.provider_instruction,
        'additionalNotes', n.additional_notes,
        'updatedAtMillis', n.updated_at,
        'deletedAtMillis', n.deleted_at
    )), '[]'::jsonb)
    INTO v_notes
    FROM public.consultation_notes n
    WHERE n.user_id = v_user_id
      AND (n.updated_at >= since_millis OR (n.deleted_at IS NOT NULL AND n.deleted_at >= since_millis));

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'id', f.id,
        'consultationId', f.consultation_id,
        'text', f.text,
        'dueDateEpochDay', f.due_date_epoch_day,
        'isCompleted', f.is_completed,
        'completedAtMillis', f.completed_at,
        'createdAtMillis', f.created_at,
        'updatedAtMillis', f.updated_at,
        'deletedAtMillis', f.deleted_at
    )), '[]'::jsonb)
    INTO v_follow_ups
    FROM public.follow_up_actions f
    WHERE f.user_id = v_user_id
      AND (f.updated_at >= since_millis OR (f.deleted_at IS NOT NULL AND f.deleted_at >= since_millis));

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'id', a.id,
        'consultationId', a.consultation_id,
        'questionId', a.question_id,
        'target', a.target,
        'kind', a.kind,
        'fileName', a.file_name,
        'mimeType', a.mime_type,
        'sizeBytes', a.size_bytes,
        'durationMs', a.duration_ms,
        'caption', a.caption,
        'createdAtMillis', a.created_at,
        'updatedAtMillis', a.updated_at,
        'deletedAtMillis', a.deleted_at
    )), '[]'::jsonb)
    INTO v_attachments
    FROM public.attachments a
    WHERE a.user_id = v_user_id
      AND (a.updated_at >= since_millis OR (a.deleted_at IS NOT NULL AND a.deleted_at >= since_millis));

    SELECT jsonb_build_object(
        'visitReminderEnabled', s.visit_reminder_enabled,
        'visitReminderHour', s.visit_reminder_hour,
        'visitThirtyMinBeforeEnabled', s.visit_30m_enabled,
        'visitAtTimeEnabled', s.visit_at_time_enabled,
        'customLeadMinutes', s.custom_lead_minutes,
        'alarmSoundModeEnabled', s.alarm_sound_mode_enabled,
        'prepareReminderEnabled', s.prepare_reminder_enabled,
        'followUpReminderEnabled', s.followup_reminder_enabled,
        'followUpReminderHour', s.followup_reminder_hour,
        'backupReminderEnabled', s.backup_reminder_enabled,
        'showTitleOnLockScreen', s.show_title_on_lock_screen,
        'updatedAtMillis', s.updated_at
    )
    INTO v_settings
    FROM public.user_settings s
    WHERE s.user_id = v_user_id
      AND s.updated_at >= since_millis;

    RETURN jsonb_build_object(
        'serverTimestampMillis', v_server_now,
        'consultations', v_consultations,
        'questions', v_questions,
        'notes', v_notes,
        'followUps', v_follow_ups,
        'attachments', v_attachments,
        'reminderSettings', v_settings
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.sync_push(jsonb) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sync_pull(bigint) TO authenticated;
