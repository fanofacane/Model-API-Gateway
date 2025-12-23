--
-- PostgreSQL database dump
--

\restrict BWebNlOi6OIsUoCAHRB8DNQ6UDF9JC0jc1g80Qkiod5n1kpbWQM8J258aAV69qN

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: api_instance_metrics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.api_instance_metrics (
    id character varying(36) NOT NULL,
    registry_id character varying(36) NOT NULL,
    timestamp_window timestamp without time zone DEFAULT date_trunc('minute'::text, now()) NOT NULL,
    success_count bigint DEFAULT 0 NOT NULL,
    failure_count bigint DEFAULT 0 NOT NULL,
    total_latency_ms bigint DEFAULT 0 NOT NULL,
    concurrency integer DEFAULT 0 NOT NULL,
    current_gateway_status character varying(32) DEFAULT 'HEALTHY'::character varying NOT NULL,
    last_reported_at timestamp without time zone DEFAULT now() NOT NULL,
    additional_metrics jsonb DEFAULT '{}'::jsonb
);


ALTER TABLE public.api_instance_metrics OWNER TO postgres;

--
-- Name: TABLE api_instance_metrics; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.api_instance_metrics IS '存储 API 实例的实时和历史调用指标，用于 Gateway 的高可用决策和智能调度';


--
-- Name: COLUMN api_instance_metrics.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.id IS '指标记录的唯一标识符 (UUID 字符串，由应用层生成)';


--
-- Name: COLUMN api_instance_metrics.registry_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.registry_id IS '关联的 API 业务实例 ID，外键关联 api_instance_registry 表';


--
-- Name: COLUMN api_instance_metrics.timestamp_window; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.timestamp_window IS '指标统计的时间窗口起始点 (YYYY-MM-DD HH:MM:00)，例如，记录从该时间点开始的 1 分钟内的聚合数据';


--
-- Name: COLUMN api_instance_metrics.success_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.success_count IS '该时间窗口内成功的 API 调用次数';


--
-- Name: COLUMN api_instance_metrics.failure_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.failure_count IS '该时间窗口内失败的 API 调用次数';


--
-- Name: COLUMN api_instance_metrics.total_latency_ms; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.total_latency_ms IS '该时间窗口内所有 API 调用的总延迟（毫秒），用于计算平均延迟';


--
-- Name: COLUMN api_instance_metrics.concurrency; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.concurrency IS '该时间窗口内观察到的最大或当前活跃并发连接数（由上报方提供，用于实时负载均衡）';


--
-- Name: COLUMN api_instance_metrics.current_gateway_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.current_gateway_status IS 'Gateway 根据内部逻辑判断的 API 实例状态：HEALTHY, DEGRADED, FAULTY, CIRCUIT_BREAKER_OPEN';


--
-- Name: COLUMN api_instance_metrics.last_reported_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.last_reported_at IS '最后一次上报数据到该指标记录的时间';


--
-- Name: COLUMN api_instance_metrics.additional_metrics; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_metrics.additional_metrics IS '额外指标，JSONB 格式 (例如：{"total_prompt_tokens": 12345, "total_completion_tokens": 67890, "total_cost": 0.123})';


--
-- Name: api_instance_registry; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.api_instance_registry (
    id character varying(36) NOT NULL,
    project_id character varying(36) NOT NULL,
    user_id character varying(64),
    api_identifier character varying(128) NOT NULL,
    api_type character varying(32) NOT NULL,
    business_id character varying(128) NOT NULL,
    routing_params jsonb DEFAULT '{}'::jsonb,
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.api_instance_registry OWNER TO postgres;

--
-- Name: TABLE api_instance_registry; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.api_instance_registry IS '存储注册到 Gateway 的后端 API 业务实例的元数据，用于智能调度决策';


--
-- Name: COLUMN api_instance_registry.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.id IS 'API 业务实例的唯一标识符 (UUID 字符串，由应用层生成)';


--
-- Name: COLUMN api_instance_registry.project_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.project_id IS '所属项目的 ID，外键关联 projects 表';


--
-- Name: COLUMN api_instance_registry.user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.user_id IS '所属用户 ID (可选)，用于用户级别的 API 资源隔离';


--
-- Name: COLUMN api_instance_registry.api_identifier; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.api_identifier IS 'API 的逻辑标识符，如 "gpt4o", "sms_sender"';


--
-- Name: COLUMN api_instance_registry.api_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.api_type IS 'API 的类型，如 "MODEL", "PAYMENT_GATEWAY", "NOTIFICATION_SERVICE"';


--
-- Name: COLUMN api_instance_registry.business_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.business_id IS '项目方内部用于识别此 API 实例的业务 ID，由 Gateway 返回给调用方';


--
-- Name: COLUMN api_instance_registry.routing_params; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.routing_params IS '影响 Gateway 调度决策的实例级参数，JSONB 格式。例如：{"priority": 100, "cost_per_unit": 0.0001, "initial_weight": 50}';


--
-- Name: COLUMN api_instance_registry.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.status IS 'API 实例的当前状态：ACTIVE (活跃), INACTIVE (非活跃), DEPRECATED (已弃用)';


--
-- Name: COLUMN api_instance_registry.metadata; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.metadata IS '额外扩展信息，JSONB 格式，供 Gateway 内部决策或未来扩展使用 (如功能特性、地域信息)';


--
-- Name: COLUMN api_instance_registry.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.created_at IS '记录创建时间';


--
-- Name: COLUMN api_instance_registry.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_instance_registry.updated_at IS '记录最后更新时间，每次更新时自动修改';


--
-- Name: api_keys; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.api_keys (
    id character varying(36) NOT NULL,
    api_key_value character varying(256) NOT NULL,
    description text,
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    issued_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone,
    last_used_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.api_keys OWNER TO postgres;

--
-- Name: TABLE api_keys; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.api_keys IS '独立管理 API Keys 及其生命周期，Key 可被项目绑定';


--
-- Name: COLUMN api_keys.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.id IS 'API Key 记录的唯一标识符 (UUID 字符串，由应用层生成)';


--
-- Name: COLUMN api_keys.api_key_value; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.api_key_value IS '实际的 API Key 字符串，必须全局唯一且安全存储';


--
-- Name: COLUMN api_keys.description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.description IS '对该 API Key 的描述，例如 "为生产环境项目A预留的Key", "测试用途Key"';


--
-- Name: COLUMN api_keys.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.status IS 'API Key 的状态：ACTIVE (激活), REVOKED (已撤销), EXPIRED (已过期), UNUSED (未使用)';


--
-- Name: COLUMN api_keys.issued_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.issued_at IS 'API Key 的颁发时间';


--
-- Name: COLUMN api_keys.expires_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.expires_at IS 'API Key 的过期时间 (可选)。如果为 NULL，则永不过期';


--
-- Name: COLUMN api_keys.last_used_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.last_used_at IS 'API Key 最后一次被使用的时间，可用于审计和清理过期/不活跃 Key';


--
-- Name: COLUMN api_keys.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.created_at IS '记录创建时间';


--
-- Name: COLUMN api_keys.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.api_keys.updated_at IS '记录最后更新时间，每次更新时自动修改';


--
-- Name: lb_active_strategy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.lb_active_strategy (
    id integer NOT NULL,
    strategy_id integer NOT NULL,
    strategy_name character varying(100) NOT NULL,
    strategy_type character varying(50) NOT NULL,
    strategy_desc text,
    operator character varying(50) NOT NULL,
    create_time timestamp without time zone DEFAULT now(),
    update_time timestamp without time zone DEFAULT now()
);


ALTER TABLE public.lb_active_strategy OWNER TO postgres;

--
-- Name: TABLE lb_active_strategy; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.lb_active_strategy IS '负载均衡生效策略表：记录当前正在生效的负载均衡策略（全局唯一）';


--
-- Name: COLUMN lb_active_strategy.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.id IS '主键ID：生效策略记录唯一标识，自增';


--
-- Name: COLUMN lb_active_strategy.strategy_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.strategy_id IS '关联策略ID：关联load_balance_strategy表的主键，指向具体策略';


--
-- Name: COLUMN lb_active_strategy.strategy_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.strategy_name IS '策略名称：冗余存储生效策略的名称（便于快速查询，无需关联主表）';


--
-- Name: COLUMN lb_active_strategy.strategy_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.strategy_type IS '策略类型：冗余存储生效策略的类型（便于快速查询）';


--
-- Name: COLUMN lb_active_strategy.strategy_desc; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.strategy_desc IS '策略描述：冗余存储生效策略的描述（便于快速查询）';


--
-- Name: COLUMN lb_active_strategy.operator; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.operator IS '操作人：管理端选择/切换生效策略的用户名/账号';


--
-- Name: COLUMN lb_active_strategy.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.create_time IS '生效时间：策略被选为生效策略的时间，默认当前时区时间';


--
-- Name: COLUMN lb_active_strategy.update_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.lb_active_strategy.update_time IS '更新时间：生效策略修改时间（如切换策略），默认当前时区时间';


--
-- Name: lb_active_strategy_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.lb_active_strategy_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.lb_active_strategy_id_seq OWNER TO postgres;

--
-- Name: lb_active_strategy_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.lb_active_strategy_id_seq OWNED BY public.lb_active_strategy.id;


--
-- Name: load_balance_strategy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.load_balance_strategy (
    id integer NOT NULL,
    strategy_name character varying(100) NOT NULL,
    strategy_type character varying(50) NOT NULL,
    strategy_desc text,
    status smallint,
    create_time timestamp without time zone DEFAULT now(),
    update_time timestamp without time zone DEFAULT now()
);


ALTER TABLE public.load_balance_strategy OWNER TO postgres;

--
-- Name: TABLE load_balance_strategy; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.load_balance_strategy IS '负载均衡策略定义表：存储所有负载均衡策略的基础信息（启用/禁用状态）';


--
-- Name: COLUMN load_balance_strategy.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.load_balance_strategy.id IS '主键ID：策略唯一标识，自增';


--
-- Name: COLUMN load_balance_strategy.strategy_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.load_balance_strategy.strategy_name IS '策略名称：便于识别，如「订单服务轮询策略」';


--
-- Name: COLUMN load_balance_strategy.strategy_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.load_balance_strategy.strategy_type IS '策略类型：如round_robin(轮询)、weighted_round_robin(加权轮询)、ip_hash(IP哈希)等';


--
-- Name: COLUMN load_balance_strategy.strategy_desc; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.load_balance_strategy.strategy_desc IS '策略描述：详细说明策略的适用场景、规则等';


--
-- Name: COLUMN load_balance_strategy.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.load_balance_strategy.status IS '策略状态：0=禁用（不可被启用），1=启用（可被选为生效策略）';


--
-- Name: COLUMN load_balance_strategy.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.load_balance_strategy.create_time IS '创建时间：策略录入时间，默认当前时区时间';


--
-- Name: COLUMN load_balance_strategy.update_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.load_balance_strategy.update_time IS '更新时间：策略信息修改时间，默认当前时区时间';


--
-- Name: load_balance_strategy_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.load_balance_strategy_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.load_balance_strategy_id_seq OWNER TO postgres;

--
-- Name: load_balance_strategy_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.load_balance_strategy_id_seq OWNED BY public.load_balance_strategy.id;


--
-- Name: projects; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.projects (
    id character varying(36) NOT NULL,
    name character varying(128) NOT NULL,
    description text,
    api_key character varying(256) NOT NULL,
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.projects OWNER TO postgres;

--
-- Name: TABLE projects; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.projects IS '管理 API-Premium Gateway 的项目信息，用于认证和多租户隔离';


--
-- Name: COLUMN projects.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.projects.id IS '项目的唯一标识符 (UUID 字符串，由应用层生成)';


--
-- Name: COLUMN projects.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.projects.name IS '项目名称，必须唯一';


--
-- Name: COLUMN projects.description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.projects.description IS '项目的详细描述';


--
-- Name: COLUMN projects.api_key; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.projects.api_key IS '用于项目认证的 API Key，必须唯一且安全存储';


--
-- Name: COLUMN projects.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.projects.status IS '项目状态：ACTIVE (活跃), INACTIVE (非活跃)';


--
-- Name: COLUMN projects.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.projects.created_at IS '记录创建时间';


--
-- Name: COLUMN projects.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.projects.updated_at IS '记录最后更新时间，每次更新时自动修改';


--
-- Name: lb_active_strategy id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.lb_active_strategy ALTER COLUMN id SET DEFAULT nextval('public.lb_active_strategy_id_seq'::regclass);


--
-- Name: load_balance_strategy id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.load_balance_strategy ALTER COLUMN id SET DEFAULT nextval('public.load_balance_strategy_id_seq'::regclass);


--
-- Data for Name: api_instance_metrics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.api_instance_metrics (id, registry_id, timestamp_window, success_count, failure_count, total_latency_ms, concurrency, current_gateway_status, last_reported_at, additional_metrics) FROM stdin;
\.


--
-- Data for Name: api_instance_registry; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.api_instance_registry (id, project_id, user_id, api_identifier, api_type, business_id, routing_params, status, metadata, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: api_keys; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.api_keys (id, api_key_value, description, status, issued_at, expires_at, last_used_at, created_at, updated_at) FROM stdin;
d35452c48f3e10a3add1a03f33e807d5	test-data-generator-key-1766406409415	测试数据生成器专用API Key	ACTIVE	2025-12-22 20:26:49.882579	\N	\N	2025-12-22 20:26:49.934935	2025-12-22 20:26:49.935936
12a84654bf6829b42bcf2df334ac6832	default-api-key-1234567890	测试数据生成器专用API Key	ACTIVE	2025-12-21 20:25:44.178414	\N	\N	2025-12-21 20:25:44.22723	2025-12-21 20:25:44.228754
\.


--
-- Data for Name: lb_active_strategy; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.lb_active_strategy (id, strategy_id, strategy_name, strategy_type, strategy_desc, operator, create_time, update_time) FROM stdin;
1	2	轮询策略	ROUND_ROBIN	按固定顺序轮询分发请求到后端所有实例，保证请求均匀分配	admin	2025-12-21 09:41:36.171771	2025-12-21 09:41:36.171771
\.


--
-- Data for Name: load_balance_strategy; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.load_balance_strategy (id, strategy_name, strategy_type, strategy_desc, status, create_time, update_time) FROM stdin;
1	智能负载均衡策略	SMART	根据后端实例的成功率、响应时间等指标自动选择最优实例，动态调整分发策略	1	2025-12-21 09:41:22.174031	2025-12-21 09:41:22.174031
3	成功率优先策略	SUCCESS_RATE_FIRST	优先分发请求到历史调用成功率最高的实例，降低失败率	1	2025-12-21 09:41:22.174031	2025-12-21 09:41:22.174031
4	延迟优先策略	LATENCY_FIRST	优先分发请求到平均响应延迟最低的实例，提升接口响应速度，适用于用户体验敏感场景	1	2025-12-21 09:41:22.174031	2025-12-21 09:41:22.174031
2	轮询策略	ROUND_ROBIN	按固定顺序轮询分发请求到后端所有实例，保证请求均匀分配	1	2025-12-21 09:41:22.174031	2025-12-21 09:41:22.174031
\.


--
-- Data for Name: projects; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.projects (id, name, description, api_key, status, created_at, updated_at) FROM stdin;
\.


--
-- Name: lb_active_strategy_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.lb_active_strategy_id_seq', 1, true);


--
-- Name: load_balance_strategy_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.load_balance_strategy_id_seq', 4, true);


--
-- Name: api_instance_metrics api_instance_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.api_instance_metrics
    ADD CONSTRAINT api_instance_metrics_pkey PRIMARY KEY (id);


--
-- Name: api_instance_registry api_instance_registry_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.api_instance_registry
    ADD CONSTRAINT api_instance_registry_pkey PRIMARY KEY (id);


--
-- Name: api_keys api_keys_api_key_value_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT api_keys_api_key_value_key UNIQUE (api_key_value);


--
-- Name: api_keys api_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT api_keys_pkey PRIMARY KEY (id);


--
-- Name: lb_active_strategy lb_active_strategy_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.lb_active_strategy
    ADD CONSTRAINT lb_active_strategy_pkey PRIMARY KEY (id);


--
-- Name: load_balance_strategy load_balance_strategy_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.load_balance_strategy
    ADD CONSTRAINT load_balance_strategy_pkey PRIMARY KEY (id);


--
-- Name: projects projects_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

\unrestrict BWebNlOi6OIsUoCAHRB8DNQ6UDF9JC0jc1g80Qkiod5n1kpbWQM8J258aAV69qN

