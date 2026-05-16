
    create table briefing_schedules (
        active boolean not null,
        id bigserial not null,
        last_run_at timestamp(6),
        category varchar(255) not null unique check (category in ('WORLD_NEWS','US_NEWS','FINANCE','TECHNOLOGY','GLOBAL_SITREP','THEATER_UKRAINE','THEATER_MIDDLE_EAST')),
        cron_expression varchar(255) not null,
        primary key (id)
    );

    create table daily_briefings (
        generated_at timestamp(6),
        id bigserial not null,
        category varchar(255) check (category in ('WORLD_NEWS','US_NEWS','FINANCE','TECHNOLOGY','GLOBAL_SITREP','THEATER_UKRAINE','THEATER_MIDDLE_EAST')),
        html_content TEXT,
        markdown_content TEXT,
        model_name varchar(255),
        primary key (id)
    );

    create table llm_configs (
        active boolean not null,
        num_ctx integer,
        id bigserial not null,
        updated_at timestamp(6),
        api_key varchar(255),
        base_url varchar(255),
        model_name varchar(255),
        name varchar(255) not null unique,
        provider varchar(255) not null check (provider in ('OLLAMA','GEMINI')),
        primary key (id)
    );

    create table macro_metrics (
        change float(53),
        change_percent float(53),
        price float(53),
        updated_at timestamp(6),
        label varchar(255),
        ticker varchar(255) not null,
        primary key (ticker)
    );

    create table market_events (
        id bigserial not null,
        timestamp timestamp(6),
        rationale TEXT,
        ticker varchar(255) not null,
        title varchar(255),
        primary key (id)
    );

    create table market_predictions (
        actual_price float(53),
        confidence_score integer,
        generation_date date,
        predicted_price float(53),
        target_date date,
        briefing_id bigint,
        id bigserial not null,
        model_name varchar(255),
        rationale TEXT,
        synthesis TEXT,
        tail_risk TEXT,
        ticker varchar(255),
        primary key (id)
    );

    create table metric_history (
        change_percent float(53),
        price float(53),
        id bigserial not null,
        timestamp timestamp(6) not null,
        ticker varchar(255) not null,
        primary key (id)
    );

    create table pipeline_runs (
        input_tokens integer,
        output_tokens integer,
        end_time timestamp(6),
        id bigserial not null,
        start_time timestamp(6),
        category varchar(255) check (category in ('WORLD_NEWS','US_NEWS','FINANCE','TECHNOLOGY','GLOBAL_SITREP','THEATER_UKRAINE','THEATER_MIDDLE_EAST')),
        error_detail TEXT,
        error_message TEXT,
        model_name varchar(255),
        status varchar(255) check (status in ('PENDING','SUCCESS','FAILED')),
        primary key (id)
    );

    create table users (
        id bigserial not null,
        password varchar(255) not null,
        role varchar(255) not null,
        username varchar(255) not null unique,
        primary key (id)
    );
