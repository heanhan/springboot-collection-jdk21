#!/usr/bin/env python3
"""从指南提取练习代码，在隔离副本验证；不修改工作区业务源码。"""

import argparse
import difflib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap
import xml.etree.ElementTree as ET


PROJECT = Path(__file__).resolve().parents[1]
MODULE = "ddd-order-service"
MAIN = f"{MODULE}/src/main/java/com/example/ddd/order/"
TEST = f"{MODULE}/src/test/java/com/example/ddd/order/"
COMMAND_IMPORT = "com.example.ddd.order.application.command.ChangeOrderRemarkCommand"

# 复用持久化测试里的真实应用事务与 H2，只模拟认证远程接口。
HTTP_IMPORTS = [
    "com.example.ddd.common.infrastructure.web.RestErrors",
    "com.example.ddd.common.result.Result",
    "com.example.ddd.contract.auth.AuthFeignClient",
    "com.example.ddd.contract.auth.CurrentUserDTO",
    "com.example.ddd.order.interfaces.rest.OrderController",
    "org.springframework.test.web.servlet.setup.MockMvcBuilders",
    "org.springframework.http.MediaType",
    "java.util.Set",
    "static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*",
    "static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*",
]
HTTP_TEST = r'''
@Test
void remarkHttpContractUsesRealApplicationAndPersistence() throws Exception {
    String id = service.placeOrder(command());
    int eventsBefore = eventCount(id);
    clearInvocations(products, inventory);
    AuthFeignClient auth = mock(AuthFeignClient.class);
    when(auth.parseToken("Bearer owner")).thenReturn(Result.ok(new CurrentUserDTO(
            "user", "学习用户", "mock-owner", Set.of(), Set.of(), Long.MAX_VALUE)));
    when(auth.parseToken("Bearer other")).thenReturn(Result.ok(new CurrentUserDTO(
            "other", "另一用户", "mock-other", Set.of(), Set.of(), Long.MAX_VALUE)));
    when(auth.parseToken("Bearer anonymous")).thenReturn(Result.ok(new CurrentUserDTO(
            " ", "无身份", "mock-anonymous", Set.of(), Set.of(), Long.MAX_VALUE)));
    ObjectMapper json = new ObjectMapper();
    var responses = new java.util.ArrayList<String>();
    var mvc = MockMvcBuilders.standaloneSetup(new OrderController(service, auth))
            .setControllerAdvice(new RestErrors())
            .alwaysDo(result -> responses.add(json.writeValueAsString(java.util.Map.of(
                    "method", result.getRequest().getMethod(),
                    "path", result.getRequest().getRequestURI(),
                    "status", result.getResponse().getStatus(),
                    "body", json.readTree(result.getResponse().getContentAsString(
                            java.nio.charset.StandardCharsets.UTF_8))))))
            .build();
    String url = "/orders/" + id + "/remark";

    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"  工作日联系  \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0000"));
    mvc.perform(get(url).header("Authorization", "Bearer owner"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.remark").value("工作日联系"));
    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"工作日联系\"}"))
            .andExpect(status().isOk());
    String boundary = "中".repeat(200);
    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(java.util.Map.of("remark", "  " + boundary + "  "))))
            .andExpect(status().isOk());
    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(java.util.Map.of("remark", "中".repeat(201)))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("0001"));
    assertThat(service.getRemark(id, "user")).isEqualTo(boundary);
    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":null}"))
            .andExpect(status().isOk());
    mvc.perform(get(url).header("Authorization", "Bearer owner"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.remark").value(""));
    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk());
    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("0001"));
    for (String actor : List.of("other", "anonymous")) {
        String code = actor.equals("other") ? "0003" : "0002";
        mvc.perform(get(url).header("Authorization", "Bearer " + actor))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code));
        mvc.perform(patch(url).header("Authorization", "Bearer " + actor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"越权修改\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code));
    }
    mvc.perform(get("/orders/missing-remark-order/remark").header("Authorization", "Bearer owner"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("5001"));
    mvc.perform(patch("/orders/missing-remark-order/remark").header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"不存在\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("5001"));
    assertThat(eventCount(id)).isEqualTo(eventsBefore);
    verifyNoInteractions(products, inventory);
    service.markPaid(id, "MOCK", "http-remark-trade", LocalDateTime.now());
    int eventsAfterPayment = eventCount(id);
    mvc.perform(patch(url).header("Authorization", "Bearer owner")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"已付后修改\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("5002"));
    mvc.perform(get(url).header("Authorization", "Bearer owner"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.remark").value(""));
    assertThat(jdbc.queryForObject("SELECT remark FROM t_order WHERE order_id=?", String.class, id)).isEmpty();
    assertThat(eventCount(id)).isEqualTo(eventsAfterPayment);

    String output = System.getProperty("tutorial.http.responses");
    if (output != null) {
        java.nio.file.Files.writeString(java.nio.file.Path.of(output), String.join("\n", responses) + "\n");
    }
}
'''


def replace_once(source, old, new, label):
    if source.count(old) != 1:
        raise ValueError(f"{label}：源码锚点不存在或不唯一；请从未实现备注功能的副本运行。")
    return source.replace(old, new, 1)


def add_imports(source, imports):
    anchor = source.splitlines()[0]
    if not anchor.startswith("package "):
        raise ValueError("未找到预期的 Java package 声明。")
    missing = [f"import {name};" for name in imports if f"import {name};" not in source]
    return replace_once(source, anchor, anchor + "\n\n" + "\n".join(missing), "添加 import")


def append_methods(source, methods):
    stripped = source.rstrip()
    if not stripped.endswith("}"):
        raise ValueError("Java 测试类末尾不符合预期，停止生成。")
    return stripped[:-1] + textwrap.indent(methods.strip(), "    ") + "\n}\n"


def read_snippet(document, name):
    marker = f"<!-- tutorial:{name} -->"
    if document.count(marker) != 1:
        raise ValueError(f"指南代码标记缺失或重复：{name}")
    match = re.search(re.escape(marker) + r"\s*```java\n(.*?)\n```", document, re.DOTALL)
    if not match:
        raise ValueError(f"指南代码块格式不正确：{name}")
    return match.group(1) + "\n"


def check_document_links():
    """检查本地 Markdown 链接、标题锚点与折叠块，不访问网络。"""
    documents = [PROJECT / "README.md", PROJECT / "docs/ddd-beginner-guide.md",
                 PROJECT / "docs/operations.md"]

    def visible_text(path):
        visible, fenced = [], False
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.startswith("```"):
                fenced = not fenced
            elif not fenced:
                visible.append(line)
        if fenced:
            raise ValueError(f"代码围栏未闭合：{path}")
        return "\n".join(visible)

    def anchors(path):
        text = visible_text(path)
        result = set(re.findall(r'<a\s+id="([^"]+)"', text))
        seen = {}
        for title in re.findall(r"^#{1,6}\s+(.+)$", text, re.MULTILINE):
            slug = re.sub(r"[^\w\s-]", "", title.strip().lower())
            slug = re.sub(r"\s", "-", slug)
            index = seen.get(slug, 0)
            seen[slug] = index + 1
            result.add(slug if index == 0 else f"{slug}-{index}")
        return result

    checked = 0
    for path in documents:
        text = visible_text(path)
        depth = 0
        for tag in re.findall(r"</?details>", text):
            depth += -1 if tag.startswith("</") else 1
            if depth < 0:
                raise ValueError(f"折叠块顺序错误：{path}")
        if depth:
            raise ValueError(f"折叠块未闭合：{path}")
        for link in re.findall(r"\[[^\]\n]+\]\(([^\s)]+)\)", text):
            if re.match(r"[a-zA-Z][\w+.-]*:", link):
                continue
            filename, _, fragment = link.partition("#")
            target = (path.parent / filename).resolve() if filename else path
            if not target.exists():
                raise ValueError(f"链接目标不存在：{path.name} → {link}")
            if fragment and target.suffix == ".md" and fragment not in anchors(target):
                raise ValueError(f"标题锚点不存在：{path.name} → {link}")
            checked += 1
    print(f"文档结构与 {checked} 个本地链接检查通过。", flush=True)


def run_maven(sandbox, name, arguments, expect_failure=False):
    log = sandbox / f"{name}.log"
    command = ["mvn", "-B", "-ntp", "-f", "springboot-ddd-project/pom.xml", *arguments]
    print(f"运行 {name}，日志：{log}", flush=True)
    with log.open("w", encoding="utf-8") as stream:
        result = subprocess.run(command, cwd=sandbox, stdout=stream, stderr=subprocess.STDOUT,
                                timeout=900, check=False)
    if expect_failure and result.returncode:
        print(f"{name}: 已观察到预期失败，接下来核对失败测试。", flush=True)
        return
    if result.returncode or expect_failure:
        print("\n".join(log.read_text(encoding="utf-8").splitlines()[-60:]), flush=True)
        raise RuntimeError(f"{name} 结果不符合预期，退出码 {result.returncode}；请检查完整日志。")
    print(f"{name}: BUILD SUCCESS", flush=True)


def report_tests(project, class_name):
    report = project / MODULE / "target/surefire-reports" / f"TEST-com.example.ddd.order.{class_name}.xml"
    suite = ET.parse(report).getroot()
    counts = {key: int(suite.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
    if not counts["tests"] or any(counts[key] for key in ("failures", "errors", "skipped")):
        raise RuntimeError(f"{class_name} 未实际通过：{counts}")
    print(f"{class_name}: {json.dumps(counts, ensure_ascii=False)}", flush=True)
    return {case.get("name") for case in suite.findall("testcase")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-docs", action="store_true", help="只检查文档结构和本地链接，不构建副本")
    args = parser.parse_args()
    check_document_links()
    if args.check_docs:
        return
    if not shutil.which("mvn"):
        raise RuntimeError("找不到 mvn，请先安装 Maven 并通过 mvn -version 确认使用 JDK 21。")
    document = (PROJECT / "docs/ddd-beginner-guide.md").read_text(encoding="utf-8")
    names = ("warmup-test", "remark-domain", "remark-domain-tests", "remark-command",
             "remark-application", "remark-controller", "remark-persistence-tests")
    snippets = {name: read_snippet(document, name) for name in names}
    paths = {
        "domain": MAIN + "domain/model/aggregate/Order.java",
        "application": MAIN + "application/service/OrderApplicationService.java",
        "controller": MAIN + "interfaces/rest/OrderController.java",
        "domain-tests": TEST + "OrderDomainTest.java",
        "persistence-tests": TEST + "OrderPersistenceTest.java",
        "command": MAIN + "application/command/ChangeOrderRemarkCommand.java",
    }
    originals = {key: (PROJECT / path).read_text(encoding="utf-8")
                 for key, path in paths.items() if key != "command"}
    if (PROJECT / paths["command"]).exists() or any(
            "changeRemark(" in originals[key] for key in ("domain", "application", "controller")):
        raise ValueError("当前副本已经包含备注实现；请在未做此练习的项目副本运行，不会覆盖现有代码。")

    generated = dict(originals)
    generated["domain"] = replace_once(generated["domain"], "private final String remark;",
                                       "private String remark;", "备注字段")
    anchor = "    /**\n     * 支付成功：CREATED → PAID。"
    generated["domain"] = replace_once(generated["domain"], anchor,
                                       textwrap.indent(snippets["remark-domain"], "    ") + "\n" + anchor,
                                       "领域方法")
    for key, anchor in (("application", "    private void publishEvents(Order order) {"),
                        ("controller", "    public static OrderDTO toDTO(Order o) {")):
        generated[key] = add_imports(generated[key], [COMMAND_IMPORT])
        generated[key] = replace_once(generated[key], anchor,
                                      textwrap.indent(snippets[f"remark-{key}"], "    ") + "\n" + anchor,
                                      key)
    warmup = originals["domain-tests"]
    if "void paidOrderCannotBeCancelled(" not in warmup:
        warmup = append_methods(warmup, snippets["warmup-test"])
    generated["domain-tests"] = append_methods(
        add_imports(warmup, ["com.example.ddd.common.exception.ErrorCode"]),
        snippets["remark-domain-tests"])
    generated["persistence-tests"] = append_methods(
        add_imports(generated["persistence-tests"], [COMMAND_IMPORT,
                    "com.example.ddd.common.exception.BusinessException",
                    "com.example.ddd.common.exception.ErrorCode", *HTTP_IMPORTS]),
        snippets["remark-persistence-tests"] + "\n" + HTTP_TEST)
    generated["command"] = snippets["remark-command"]

    # 每次创建全新的副本；不复用 target，避免旧报告被误认作本次验证结果。
    sandbox = Path(tempfile.mkdtemp(prefix="ddd-remark-tutorial-"))
    copied = sandbox / PROJECT.name
    print(f"隔离目录（运行后保留）：{sandbox}", flush=True)
    shutil.copy2(PROJECT.parent / "pom.xml", sandbox / "pom.xml")
    shutil.copytree(PROJECT, copied,
                    ignore=shutil.ignore_patterns("target", ".git", ".idea", "docs", "scripts"))
    test_args = ["-pl", MODULE, "-am", "-Dtest=OrderDomainTest",
                 "-Dsurefire.failIfNoSpecifiedTests=false", "test"]
    run_maven(sandbox, "baseline", test_args)
    report_tests(copied, "OrderDomainTest")
    wrong_expectation = replace_once(warmup,
                                    "assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);",
                                    "assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);",
                                    "热身错误预期")
    (copied / paths["domain-tests"]).write_text(wrong_expectation, encoding="utf-8")
    run_maven(sandbox, "warmup-failure", test_args, expect_failure=True)
    report = copied / MODULE / "target/surefire-reports/TEST-com.example.ddd.order.OrderDomainTest.xml"
    suite = ET.parse(report).getroot()
    failed = [case.get("name") for case in suite.findall("testcase") if case.find("failure") is not None]
    if failed != ["paidOrderCannotBeCancelled"] or int(suite.get("errors", "0")) != 0:
        raise RuntimeError("热身没有按预期在错误的状态断言处失败，请检查日志。")
    (copied / paths["domain-tests"]).write_text(warmup, encoding="utf-8")
    run_maven(sandbox, "warmup", test_args)
    if "paidOrderCannotBeCancelled" not in report_tests(copied, "OrderDomainTest"):
        raise RuntimeError("热身测试未执行，不能继续声称验证通过。")

    patch = []
    for key, content in generated.items():
        destination = copied / paths[key]
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(content, encoding="utf-8")
        relative = f"{PROJECT.name}/{paths[key]}"
        patch.extend(difflib.unified_diff(originals.get(key, "").splitlines(keepends=True),
                                         content.splitlines(keepends=True),
                                         fromfile=f"a/{relative}" if key in originals else "/dev/null",
                                         tofile=f"b/{relative}"))
    (sandbox / "order-remark.patch").write_text("".join(patch), encoding="utf-8")
    response_file = sandbox / "http-responses.jsonl"
    run_maven(sandbox, "verify", [f"-Dtutorial.http.responses={response_file}", "verify"])
    domain_names = report_tests(copied, "OrderDomainTest")
    persistence_names = report_tests(copied, "OrderPersistenceTest")
    for marker, executed in (("remark-domain-tests", domain_names),
                             ("remark-persistence-tests", persistence_names)):
        expected = set(re.findall(r"void\s+(\w+)\s*\(", snippets[marker]))
        if not expected or not expected.issubset(executed):
            raise RuntimeError(f"{marker} 中有测试没有执行。")
    if "remarkHttpContractUsesRealApplicationAndPersistence" not in persistence_names:
        raise RuntimeError("MockMvc 验证未执行。")
    responses = [json.loads(line) for line in response_file.read_text(encoding="utf-8").splitlines()]
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    for report in copied.glob("*/target/surefire-reports/TEST-*.xml"):
        suite = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, "0"))
    for key, before in originals.items():
        if (PROJECT / paths[key]).read_text(encoding="utf-8") != before:
            raise RuntimeError("验证期间工作区源文件发生变化，请单独检查；脚本仅写隔离副本。")
    if (PROJECT / paths["command"]).exists():
        raise RuntimeError("验证期间工作区新增了备注 Command，请单独检查。")
    print(f"全模块测试汇总：{json.dumps(totals, ensure_ascii=False)}", flush=True)
    print(f"MockMvc 已断言并捕获 {len(responses)} 组 HTTP 响应。", flush=True)
    print(f"参考补丁：{sandbox / 'order-remark.patch'}", flush=True)
    print(f"响应证据：{response_file}", flush=True)
    print("验证通过；工作区对应业务源码未变化。未验证真实 HTTP/MQ、真实认证或 MySQL 并发。", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired, ET.ParseError) as error:
        print(f"验证停止：{error}", file=sys.stderr)
        sys.exit(1)
