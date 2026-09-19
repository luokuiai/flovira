// @vitest-environment jsdom

import { createRef, useState } from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, describe, expect, test } from "vitest";
import { DEMO_CAPABILITIES } from "../../examples/capabilities";
import { insertCanvasNode, setBranchRule } from "./branchConditions";
import { ReactFlowDesigner } from "./ReactFlowDesigner";
import {
  createInitialDefinition,
  getApproverRule,
  insertNodeAfter,
  setApproverRule,
  setSubmitterRule,
} from "./model";
import type {
  DesignerInputProps,
  DesignerTooltipProps,
  ReactFlowDesignerRef,
} from "./types";

afterEach(cleanup);

describe("ReactFlowDesigner", () => {
  test.each([false, true])(
    "shows submitter scope and system end content (readonly=%s)",
    (disabled) => {
      const definition = createInitialDefinition();
      const start = definition.nodeList.find((node) => node.nodeType === "0")!;
      const end = definition.nodeList.find((node) => node.nodeType === "2")!;
      const view = render(
        <ReactFlowDesigner value={definition} disabled={disabled} />,
      );
      expect(
        within(
          view.getByRole("button", { name: `编辑节点：${start.nodeName}` }),
        ).getByText("全员"),
      ).toBeTruthy();
      expect(
        within(
          view.getByRole("button", { name: `编辑节点：${end.nodeName}` }),
        ).getByText("系统"),
      ).toBeTruthy();
      for (const strategy of ["USER", "ROLE"]) {
        const configured = {
          ...definition,
          nodeList: definition.nodeList.map((node) =>
            node === start
              ? setSubmitterRule(node, strategy, [
                  {
                    id: "allowed",
                    name: strategy === "USER" ? "张三" : "财务人员",
                    type: strategy,
                  },
                ])
              : node,
          ),
        };
        view.rerender(
          <ReactFlowDesigner value={configured} disabled={disabled} />,
        );
        expect(
          within(
            view.getByRole("button", { name: `编辑节点：${start.nodeName}` }),
          ).getByText(strategy === "USER" ? "张三" : "财务人员"),
        ).toBeTruthy();
      }
    },
  );

  test("keeps readonly arrows from start and into end", () => {
    const view = render(
      <ReactFlowDesigner defaultValue={createInitialDefinition()} disabled />,
    );
    expect(
      view.container.querySelectorAll(".frd-connector--readonly-arrow"),
    ).toHaveLength(2);
    expect(
      view.container.querySelectorAll(".frd-connector--readonly"),
    ).toHaveLength(0);
  });

  test("adds nodes at inner and outer merge exits and supports undo", () => {
    const initial = createInitialDefinition();
    const approval = initial.nodeList.find((node) => node.nodeType === "1")!;
    const outerDefinition = insertCanvasNode(initial, approval.nodeCode, "3");
    const outer = outerDefinition.nodeList.find(
      (node) => node.nodeType === "3",
    )!;
    const definition = insertCanvasNode(
      outerDefinition,
      outer.nodeCode,
      "4",
      0,
    );
    const inner = definition.nodeList.find((node) => node.nodeType === "4")!;
    const ref = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner
        ref={ref}
        defaultValue={definition}
        capabilities={DEMO_CAPABILITIES}
      />,
    );
    expect(view.container.querySelectorAll(".frd-merge-flow")).toHaveLength(2);
    for (const split of [inner, outer]) {
      fireEvent.click(
        view.getByRole("button", {
          name: `在 ${split.nodeName} 合流后添加节点`,
        }),
      );
      fireEvent.click(view.getByRole("menuitem", { name: "添加审批节点" }));
      const next = ref.current!.getDefinition();
      expect(next.nodeList).toHaveLength(definition.nodeList.length + 1);
      const added = next.nodeList.find(
        (node) =>
          !definition.nodeList.some((old) => old.nodeCode === node.nodeCode),
      )!;
      expect(added.skipList[0].targetNodeCode).toBe(
        initial.nodeList.find((node) => node.nodeType === "2")!.nodeCode,
      );
      act(() => ref.current!.undo());
      expect(ref.current!.getDefinition()).toEqual(definition);
    }
    view.rerender(
      <ReactFlowDesigner ref={ref} defaultValue={definition} disabled />,
    );
    expect(view.queryByRole("button", { name: /合流后添加节点/ })).toBeNull();
  });

  test("keeps branch connectors and merge tails in readonly mode without insert controls", () => {
    const initial = createInitialDefinition();
    const definition = insertCanvasNode(
      initial,
      initial.nodeList.find((node) => node.nodeType === "0")!.nodeCode,
      "4",
    );
    const view = render(
      <ReactFlowDesigner defaultValue={definition} disabled />,
    );
    const connectors = view.container.querySelectorAll(".frd-connector--short");
    expect(connectors.length).toBeGreaterThan(0);
    connectors.forEach((connector) =>
      expect(
        connector.matches(
          ".frd-connector--readonly, .frd-connector--readonly-arrow",
        ),
      ).toBe(true),
    );
    expect(
      view.container.querySelectorAll(".frd-branch__merge-tail"),
    ).toHaveLength(2);
    expect(
      view.container.querySelector(
        ".frd-merge-flow .frd-connector--readonly-arrow",
      ),
    ).toBeTruthy();
    expect(
      view.container.querySelector(".frd-insert-point__trigger"),
    ).toBeNull();
    view.rerender(<ReactFlowDesigner defaultValue={definition} />);
    expect(view.container.querySelector(".frd-connector--readonly")).toBeNull();
    expect(
      view.container.querySelector(".frd-insert-point__trigger"),
    ).toBeTruthy();
  });

  test.each(["3", "5"] as const)(
    "marks only incomplete branches of gateway %s",
    (type) => {
      const initial = createInitialDefinition();
      const definition = insertCanvasNode(
        initial,
        initial.nodeList.find((node) => node.nodeType === "0")!.nodeCode,
        type,
      );
      const gateway = definition.nodeList.find(
        (node) => node.nodeType === type,
      )!;
      const ref = createRef<ReactFlowDesignerRef>();
      const view = render(
        <ReactFlowDesigner
          ref={ref}
          defaultValue={definition}
          capabilities={DEMO_CAPABILITIES}
        />,
      );
      expect(
        view.getByRole("img", { name: "分支配置待完善：分支一 未设置条件" }),
      ).toBeTruthy();
      expect(
        view.queryByRole("img", { name: /其他条件 未设置条件/ }),
      ).toBeNull();
      expect(view.getAllByRole("img", { name: /分支配置待完善/ })).toHaveLength(
        type === "3" ? 1 : 2,
      );
      const configured = setBranchRule(gateway, 0, "分支一", {
        mode: "expression",
        groups: [],
        expression: "spel@@#{true}",
      });
      act(() =>
        ref.current!.importJson({
          ...definition,
          nodeList: definition.nodeList.map((node) =>
            node.nodeCode === gateway.nodeCode ? configured : node,
          ),
        }),
      );
      expect(
        view.queryByRole("img", { name: "分支配置待完善：分支一 未设置条件" }),
      ).toBeNull();
      act(() => ref.current!.undo());
      expect(
        view.getByRole("img", { name: "分支配置待完善：分支一 未设置条件" }),
      ).toBeTruthy();
    },
  );

  test("does not mark parallel branches as missing conditions", () => {
    const initial = createInitialDefinition();
    const definition = insertCanvasNode(
      initial,
      initial.nodeList.find((node) => node.nodeType === "0")!.nodeCode,
      "4",
    );
    const view = render(<ReactFlowDesigner defaultValue={definition} />);
    expect(view.queryByRole("img", { name: /分支配置待完善/ })).toBeNull();
  });

  test("marks incomplete nodes and clears the marker when configured", () => {
    const initial = createInitialDefinition();
    const ref = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner
        ref={ref}
        defaultValue={initial}
        capabilities={DEMO_CAPABILITIES}
        appearance="embedded"
      />,
    );
    const approval = initial.nodeList.find((node) => node.nodeType === "1")!;
    const card = view.getByRole("button", {
      name: `编辑节点：${approval.nodeName}`,
    });
    expect(
      within(card).getByRole("img", {
        name: /配置待完善.*未选择后端支持的人员策略/,
      }),
    ).toBeTruthy();
    expect(view.queryByText(/项配置待完善/)).toBeNull();
    expect(ref.current!.validate().valid).toBe(false);
    expect(
      within(view.getByRole("button", { name: "编辑节点：开始" })).queryByRole(
        "img",
      ),
    ).toBeNull();
    const configured = {
      ...initial,
      nodeList: initial.nodeList.map((node) =>
        node.nodeCode === approval.nodeCode
          ? setApproverRule(node, "USER", [{ id: "u1", type: "USER" }])
          : node,
      ),
    };
    act(() => ref.current!.importJson(configured));
    expect(view.queryByRole("img", { name: /配置待完善/ })).toBeNull();
    const withWait = insertNodeAfter(configured, approval.nodeCode, "7");
    act(() => ref.current!.importJson(withWait));
    expect(
      view.getByRole("img", { name: /配置待完善.*等待标识无效/ }),
    ).toBeTruthy();
    act(() => ref.current!.undo());
    expect(view.queryByRole("img", { name: /配置待完善/ })).toBeNull();
  });

  test("floats embedded history controls independently of bottom-left zoom", () => {
    const ref = createRef<ReactFlowDesignerRef>();
    const initial = createInitialDefinition();
    const view = render(
      <ReactFlowDesigner
        ref={ref}
        appearance="embedded"
        defaultValue={initial}
      />,
    );
    expect(view.queryByRole("banner")).toBeNull();
    const history = view.container.querySelector(".frd-history-controls")!;
    expect(history.closest(".frd-canvas-shell")).toBeTruthy();
    expect(history.closest(".flovira-react-canvas")).toBeNull();
    expect(
      within(history as HTMLElement)
        .getByRole("button", { name: "撤销" })
        .hasAttribute("disabled"),
    ).toBe(true);
    act(() =>
      ref.current!.importJson({ ...initial, flowName: "修改后的流程" }),
    );
    fireEvent.click(
      within(history as HTMLElement).getByRole("button", { name: "撤销" }),
    );
    expect(ref.current!.getDefinition().flowName).toBe(initial.flowName);
    fireEvent.click(
      within(history as HTMLElement).getByRole("button", { name: "重做" }),
    );
    expect(ref.current!.getDefinition().flowName).toBe("修改后的流程");
    expect(view.container.querySelector(".frd-zoom-controls")).toBeTruthy();
    view.rerender(
      <ReactFlowDesigner ref={ref} appearance="embedded" toolbar={false} />,
    );
    expect(view.container.querySelector(".frd-history-controls")).toBeNull();
    expect(view.getByRole("button", { name: "放大" })).toBeTruthy();
    view.rerender(
      <ReactFlowDesigner ref={ref} appearance="embedded" disabled />,
    );
    expect(
      view.getByRole("button", { name: "撤销" }).hasAttribute("disabled"),
    ).toBe(true);
    view.rerender(<ReactFlowDesigner ref={ref} appearance="standalone" />);
    expect(view.getByRole("banner")).toBeTruthy();
    expect(view.container.querySelector(".frd-history-controls")).toBeNull();
    expect(ref.current!.getDefinition().flowName).toBe("修改后的流程");
  });

  test("shows only the toolbar title and preserves the unsaved indicator", () => {
    const ref = createRef<ReactFlowDesignerRef>();
    const definition = {
      ...createInitialDefinition(),
      flowName: "费用审批",
      flowCode: "expense_approval",
    };
    const view = render(
      <ReactFlowDesigner ref={ref} defaultValue={definition} />,
    );
    const toolbar = within(view.getByRole("banner"));
    expect(toolbar.getByRole("heading", { name: "费用审批" })).toBeTruthy();
    expect(toolbar.queryByText("expense_approval")).toBeNull();
    expect(toolbar.queryByLabelText("有未保存修改")).toBeNull();
    act(() => ref.current!.importJson({ ...definition, flowCode: "" }));
    expect(toolbar.queryByText("未设置流程编码")).toBeNull();
    expect(toolbar.queryByRole("paragraph")).toBeNull();
    expect(toolbar.getByLabelText("有未保存修改")).toBeTruthy();
    act(() => ref.current!.resetDirty());
    expect(toolbar.queryByLabelText("有未保存修改")).toBeNull();
  });

  test("has no implicit strategy and preserves an unsupported persisted code", () => {
    const definition = createInitialDefinition();
    definition.nodeList = definition.nodeList.map((node) =>
      node.nodeType === "1"
        ? setApproverRule(
            node,
            "HOST_REMOVED",
            [],
            "",
            undefined,
            "RELATION",
            { key: "kept" },
            4,
          )
        : node,
    );
    const ref = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner ref={ref} defaultValue={definition} />,
    );
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    expect(
      view.getByRole("option", { name: "不支持的策略：HOST_REMOVED" }),
    ).toBeTruthy();
    expect(view.queryByRole("option", { name: "用户" })).toBeNull();
    expect(ref.current?.validate().issues).toContainEqual(
      expect.objectContaining({ code: "APPROVER_STRATEGY_UNKNOWN" }),
    );
    expect(
      getApproverRule(
        ref
          .current!.getDefinition()
          .nodeList.find((node) => node.nodeCode === approval.nodeCode)!,
      ),
    ).toMatchObject({
      strategy: "HOST_REMOVED",
      strategyVersion: 4,
      config: { key: "kept" },
    });
  });

  test("revalidates persisted versions when backend capabilities arrive", () => {
    const definition = createInitialDefinition();
    definition.nodeList = definition.nodeList.map((node) =>
      node.nodeType === "1"
        ? setApproverRule(
            node,
            "USER",
            [{ id: "a", type: "USER" }],
            "",
            undefined,
            "RESOURCE",
            {},
            2,
          )
        : node,
    );
    const ref = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner ref={ref} defaultValue={definition} />,
    );
    view.rerender(
      <ReactFlowDesigner
        ref={ref}
        defaultValue={definition}
        capabilities={DEMO_CAPABILITIES}
      />,
    );
    expect(ref.current?.validate().issues).toContainEqual(
      expect.objectContaining({ code: "APPROVER_STRATEGY_VERSION" }),
    );
    view.rerender(
      <ReactFlowDesigner
        ref={ref}
        defaultValue={definition}
        capabilities={{
          ...DEMO_CAPABILITIES,
          approverStrategies: DEMO_CAPABILITIES.approverStrategies.map(
            (item) => ({ ...item, version: 2 }),
          ),
        }}
      />,
    );
    expect(
      ref.current
        ?.validate()
        .issues.some((issue) => issue.code.startsWith("APPROVER_STRATEGY")),
    ).toBe(false);
  });

  test("switches shell appearance independently of the toolbar without resetting the draft", () => {
    const ref = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner {...{ capabilities: DEMO_CAPABILITIES }} ref={ref} />,
    );
    expect(
      view.container.querySelector("section")?.getAttribute("data-appearance"),
    ).toBe("standalone");
    act(() =>
      ref.current!.importJson({
        ...createInitialDefinition(),
        flowName: "嵌入草稿",
      }),
    );
    view.rerender(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        ref={ref}
        appearance="embedded"
        toolbar={false}
      />,
    );
    expect(
      view.container.querySelector("section")?.getAttribute("data-appearance"),
    ).toBe("embedded");
    expect(view.container.querySelector(".frd-header")).toBeNull();
    expect(ref.current!.getDefinition().flowName).toBe("嵌入草稿");
    expect(ref.current!.isDirty()).toBe(true);
    view.rerender(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        ref={ref}
        appearance="standalone"
      />,
    );
    expect(view.container.querySelector(".frd-header")).toBeTruthy();
    expect(ref.current!.getDefinition().flowName).toBe("嵌入草稿");
  });
  test("keeps the toolbar optional and exposes only editing APIs", () => {
    const ref = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner {...{ capabilities: DEMO_CAPABILITIES }} ref={ref} />,
    );
    expect(view.queryByRole("button", { name: "保存" })).toBeNull();
    expect(view.queryByRole("button", { name: "发布" })).toBeNull();
    expect(ref.current).not.toHaveProperty("save");
    expect(ref.current).not.toHaveProperty("publish");
    view.rerender(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        ref={ref}
        toolbar={false}
      />,
    );
    expect(view.container.querySelector(".frd-header")).toBeNull();
    expect(ref.current!.getFlowJson()).toBeTruthy();
    expect(ref.current!.validate()).toHaveProperty("valid");
    view.rerender(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        ref={ref}
        renderToolbar={({ defaultToolbar, dirty }) => (
          <div>
            {defaultToolbar}
            <button type="button" onClick={() => ref.current!.resetDirty()}>
              业务操作 {String(dirty)}
            </button>
          </div>
        )}
      />,
    );
    act(() =>
      ref.current!.importJson({
        ...createInitialDefinition(),
        flowName: "业务草稿",
      }),
    );
    expect(ref.current!.isDirty()).toBe(true);
    expect(view.getByRole("button", { name: "业务操作 true" })).toBeTruthy();
    fireEvent.click(view.getByRole("button", { name: "业务操作 true" }));
    expect(ref.current!.isDirty()).toBe(false);
    expect(view.getByRole("button", { name: "撤销" })).toBeTruthy();
  });

  test("leaves package import and export actions to the host", () => {
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        defaultValue={createInitialDefinition()}
      />,
    );
    expect(view.queryByRole("button", { name: "导入 JSON" })).toBeNull();
    expect(view.queryByRole("button", { name: "导出 JSON" })).toBeNull();
    expect(view.container.querySelector('input[type="file"]')).toBeNull();
  });

  test("shows multi approval only for multiple distinct specified people", () => {
    for (const ids of [[], ["a"], ["a", "b"]]) {
      const definition = createInitialDefinition();
      definition.nodeList = definition.nodeList.map((node) =>
        node.nodeType === "1"
          ? setApproverRule(
              node,
              "USER",
              ids.map((id) => ({ id, type: "USER" })),
            )
          : node,
      );
      const view = render(
        <ReactFlowDesigner
          {...{ capabilities: DEMO_CAPABILITIES }}
          defaultValue={definition}
        />,
      );
      fireEvent.click(view.getByRole("button", { name: "编辑节点：审批节点" }));
      expect(
        Boolean(view.queryByRole("combobox", { name: "多人审批策略" })),
      ).toBe(new Set(ids).size > 1);
      view.unmount();
    }
  });

  test("configures vote ratios and synchronizes runtime approval modes", () => {
    const ref = createRef<ReactFlowDesignerRef>();
    const definition = createInitialDefinition();
    definition.nodeList = definition.nodeList.map((node) =>
      node.nodeType === "1"
        ? setApproverRule(node, "USER", [
            { id: "a", type: "USER" },
            { id: "b", type: "USER" },
          ])
        : node,
    );
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        ref={ref}
        defaultValue={definition}
      />,
    );
    fireEvent.click(view.getByRole("button", { name: "编辑节点：审批节点" }));
    fireEvent.change(view.getByRole("combobox", { name: "多人审批策略" }), {
      target: { value: "VOTE" },
    });
    let ratio = view.getByLabelText("通过比例（%）") as HTMLInputElement;
    const currentNode = () =>
      ref
        .current!.getDefinition()
        .nodeList.find((node) => node.nodeType === "1")!;
    expect(ratio.value).toBe("60");
    expect(ratio.min).toBe("1");
    const modeField = view
      .getByRole("combobox", { name: "多人审批策略" })
      .closest(".frd-field")!;
    expect(modeField.nextElementSibling?.contains(ratio)).toBe(true);
    expect(view.queryByText(/同意人数占比达到此比例即通过/)).toBeNull();
    fireEvent.change(ratio, { target: { value: "75" } });
    fireEvent.click(view.getByRole("button", { name: "确定" }));
    expect(
      JSON.parse(ref.current!.getFlowJson()).nodeList.find(
        (node: { nodeType: string }) => node.nodeType === "1",
      ).nodeRatio,
    ).toBe("75");
    fireEvent.click(view.getByRole("button", { name: "编辑节点：审批节点" }));
    ratio = view.getByLabelText("通过比例（%）") as HTMLInputElement;
    fireEvent.change(ratio, { target: { value: "" } });
    expect(view.getByRole("alert").textContent).toContain("小于 100");
    for (const value of ["0", "0.5"]) {
      fireEvent.change(ratio, { target: { value } });
      expect(view.getByRole("alert").textContent).toContain("大于等于 1");
    }
    fireEvent.change(ratio, { target: { value: "1" } });
    expect(view.queryByRole("alert")).toBeNull();
    fireEvent.change(ratio, { target: { value: "100" } });
    expect(view.getByRole("alert").textContent).toContain("小于 100");
    fireEvent.change(ratio, { target: { value: "60" } });
    expect(
      ref
        .current!.validate()
        .issues.some((issue) => issue.code === "VOTE_RATIO_INVALID"),
    ).toBe(false);
    fireEvent.change(view.getByRole("combobox", { name: "多人审批策略" }), {
      target: { value: "COUNTERSIGN" },
    });
    fireEvent.click(view.getByRole("button", { name: "确定" }));
    expect(currentNode().nodeRatio).toBe("100");
    expect(view.queryByLabelText("通过比例（%）")).toBeNull();
    fireEvent.click(view.getByRole("button", { name: "编辑节点：审批节点" }));
    fireEvent.change(view.getByRole("combobox", { name: "多人审批策略" }), {
      target: { value: "OR" },
    });
    fireEvent.click(view.getByRole("button", { name: "确定" }));
    expect(currentNode().nodeRatio).toBe("0");
  });

  test("keeps subprocess providers isolated per designer instance", async () => {
    const initial = createInitialDefinition();
    const approval = initial.nodeList.find((node) => node.nodeType === "1")!;
    const definition = insertNodeAfter(initial, approval.nodeCode, "6");
    const subprocess = definition.nodeList.find(
      (node) => node.nodeType === "6",
    )!;

    const first = render(
      <ReactFlowDesigner
        defaultValue={definition}
        queryResources={async () => ({
          items: [
            {
              id: "flow_a",
              code: "flow_a",
              name: "租户 A 子流程",
              resourceType: "SUBPROCESS",
            },
          ],
          total: 1,
        })}
      />,
    );
    const second = render(
      <ReactFlowDesigner
        defaultValue={definition}
        queryResources={async () => ({
          items: [
            {
              id: "flow_b",
              code: "flow_b",
              name: "租户 B 子流程",
              resourceType: "SUBPROCESS",
            },
          ],
          total: 1,
        })}
      />,
    );

    fireEvent.click(
      within(first.container).getByRole("button", {
        name: `编辑节点：${subprocess.nodeName}`,
      }),
    );
    fireEvent.click(
      within(second.container).getByRole("button", {
        name: `编辑节点：${subprocess.nodeName}`,
      }),
    );
    fireEvent.click(
      within(first.container).getByRole("button", { name: "子流程" }),
    );
    fireEvent.click(
      within(second.container).getByRole("button", { name: "子流程" }),
    );

    await waitFor(() => {
      expect(
        within(first.container).getByRole("option", { name: "租户 A 子流程" }),
      ).toBeTruthy();
      expect(
        within(second.container).getByRole("option", { name: "租户 B 子流程" }),
      ).toBeTruthy();
    });
    expect(
      within(first.container).queryByRole("option", { name: "租户 B 子流程" }),
    ).toBeNull();
    expect(
      within(second.container).queryByRole("option", { name: "租户 A 子流程" }),
    ).toBeNull();
  });

  test("restores an edited definition through the imperative undo API", () => {
    const definition = createInitialDefinition();
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const designerRef = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        ref={designerRef}
        defaultValue={definition}
      />,
    );

    const current = within(view.container);
    fireEvent.click(
      current.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    fireEvent.change(current.getByLabelText("节点名称"), {
      target: { value: "部门负责人审批" },
    });
    fireEvent.click(current.getByRole("button", { name: "确定" }));
    expect(
      designerRef.current
        ?.getDefinition()
        .nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName,
    ).toBe("部门负责人审批");

    act(() => designerRef.current?.undo());
    expect(
      designerRef.current
        ?.getDefinition()
        .nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName,
    ).toBe("审批节点");
  });

  test("keeps undo history when a controlled host echoes onChange", () => {
    const initial = createInitialDefinition();
    const approval = initial.nodeList.find((node) => node.nodeType === "1")!;
    const designerRef = createRef<ReactFlowDesignerRef>();
    const Controlled = () => {
      const [value, setValue] = useState(initial);
      return (
        <ReactFlowDesigner
          ref={designerRef}
          value={value}
          onChange={({ definition }) => setValue(definition)}
        />
      );
    };
    const view = render(<Controlled />);
    const current = within(view.container);

    fireEvent.click(
      current.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    fireEvent.change(current.getByLabelText("节点名称"), {
      target: { value: "受控审批" },
    });
    act(() => designerRef.current?.undo());

    expect(
      designerRef.current
        ?.getDefinition()
        .nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName,
    ).toBe("审批节点");
  });

  test("uses an injected UI adapter without changing designer behavior", () => {
    const definition = createInitialDefinition();
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const designerRef = createRef<ReactFlowDesignerRef>();
    const AdapterInput = ({
      value,
      disabled,
      ariaLabel,
      onValueChange,
    }: DesignerInputProps) => (
      <input
        data-adapter="custom"
        value={value}
        disabled={disabled}
        aria-label={ariaLabel}
        onChange={(event) => onValueChange(event.target.value)}
      />
    );
    const AdapterTooltip = ({ content, children }: DesignerTooltipProps) => (
      <span data-adapter-tooltip={String(content)}>{children}</span>
    );
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        ui={{ Input: AdapterInput, Tooltip: AdapterTooltip }}
      />,
    );
    const current = within(view.container);

    expect(
      current
        .getByLabelText("撤销")
        .closest("[data-adapter-tooltip]")
        ?.getAttribute("data-adapter-tooltip"),
    ).toBe("撤销");
    fireEvent.click(
      current.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    const nameInput = current.getByLabelText("节点名称");
    expect(nameInput.getAttribute("data-adapter")).toBe("custom");
    fireEvent.change(nameInput, { target: { value: "适配器审批" } });
    fireEvent.click(current.getByRole("button", { name: "确定" }));

    expect(
      designerRef.current
        ?.getDefinition()
        .nodeList.find((node) => node.nodeCode === approval.nodeCode)?.nodeName,
    ).toBe("适配器审批");
  });

  test("opens node settings in a closable drawer", () => {
    const definition = createInitialDefinition();
    definition.nodeList = definition.nodeList.map((node) =>
      node.nodeType === "1"
        ? setApproverRule(node, "USER", [
            { id: "a", type: "USER" },
            { id: "b", type: "USER" },
          ])
        : node,
    );
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        defaultValue={definition}
      />,
    );

    expect(view.queryByRole("dialog", { name: "节点设置" })).toBeNull();
    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    expect(view.getByRole("dialog", { name: "节点设置" })).toBeTruthy();
    expect(view.getByRole("option", { name: "或签" })).toBeTruthy();
    const mode = view.getByRole("combobox", {
      name: "多人审批策略",
    }) as HTMLSelectElement;
    fireEvent.change(mode, { target: { value: "COUNTERSIGN" } });
    expect(mode.value).toBe("COUNTERSIGN");
    expect(view.getByRole("radio", { name: "驳回至上一节点" })).toBeTruthy();
    expect(view.getByRole("radio", { name: "驳回时选择节点" })).toBeTruthy();

    const closeButtons = view.getAllByRole("button", { name: "关闭节点设置" });
    fireEvent.click(closeButtons[closeButtons.length - 1]);
    expect(view.queryByRole("dialog", { name: "节点设置" })).toBeNull();
  });

  test("deletes editable nodes from the card header instead of the drawer", () => {
    const definition = createInitialDefinition();
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        defaultValue={definition}
      />,
    );

    expect(view.queryByRole("button", { name: "删除节点：开始" })).toBeNull();
    expect(view.queryByRole("button", { name: "删除节点：结束" })).toBeNull();
    fireEvent.click(
      view.getByRole("button", { name: `删除节点：${approval.nodeName}` }),
    );

    expect(
      view.queryByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    ).toBeNull();
    expect(view.queryByRole("dialog", { name: "节点设置" })).toBeNull();
  });

  test("renders arbitrary approver strategies returned by the backend", async () => {
    const definition = createInitialDefinition();
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const designerRef = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        capabilities={{
          schemaVersion: 1,
          nodeTypes: ["0", "1", "2", "3", "4", "5", "6", "7", "8"],
          approverStrategies: [
            {
              code: "CUSTOM_MANAGER_CHAIN",
              name: "自定义负责人链",
              selectionType: "RELATION",
              relationType: "CUSTOM_MANAGER_CHAIN",
              multiple: false,
              editorType: "NONE",
              resultCardinality: "ZERO_OR_ONE",
              options: [
                {
                  code: "emptyPolicy",
                  name: "无人审批策略",
                  defaultValue: "FAIL",
                  choices: [
                    { value: "FAIL", label: "阻止提交" },
                    { value: "TO_ADMIN", label: "转交管理员" },
                  ],
                },
              ],
            },
          ],
          approvalModes: ["OR"],
          returnPolicies: ["PREVIOUS"],
          timeoutNodeTypes: ["1"],
          operations: ["SAVE"],
          resourceTypes: [],
        }}
      />,
    );

    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );

    await waitFor(() =>
      expect(view.getByRole("option", { name: "自定义负责人链" })).toBeTruthy(),
    );
    expect(
      view.queryByText("运行时由后端“自定义负责人链”人员解析器确定办理人"),
    ).toBeNull();
    expect(
      view.queryByRole("button", { name: "选择自定义负责人链" }),
    ).toBeNull();
    fireEvent.change(view.getByLabelText("审批人"), {
      target: { value: "CUSTOM_MANAGER_CHAIN" },
    });
    fireEvent.click(view.getByRole("radio", { name: "转交管理员" }));
    fireEvent.click(view.getByRole("button", { name: "确定" }));
    const current = designerRef.current
      ?.getDefinition()
      .nodeList.find((node) => node.nodeCode === approval.nodeCode);
    expect(current && getApproverRule(current).config).toEqual({
      emptyPolicy: "TO_ADMIN",
    });
  });

  test("renders an inline business editor and persists custom rule config", () => {
    const definition = createInitialDefinition();
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const designerRef = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        capabilities={{
          schemaVersion: 1,
          nodeTypes: ["0", "1", "2"],
          approverStrategies: [
            {
              code: "FORM_RULE",
              name: "表单规则",
              selectionType: "RELATION",
              relationType: "FORM_RULE",
              multiple: false,
              editorType: "INLINE",
              editorKey: "form-rule-editor",
            },
          ],
          approvalModes: ["OR"],
          returnPolicies: ["PREVIOUS"],
          timeoutNodeTypes: [],
          operations: ["SAVE"],
          resourceTypes: [],
        }}
        renderApproverEditor={({ strategy, rule, onRuleChange }) =>
          strategy.editorKey === "form-rule-editor" ? (
            <button
              type="button"
              onClick={() =>
                onRuleChange({ ...rule, config: { field: "ownerId" } })
              }
            >
              配置表单规则
            </button>
          ) : null
        }
      />,
    );

    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    fireEvent.change(view.getByLabelText("审批人"), {
      target: { value: "FORM_RULE" },
    });
    fireEvent.click(view.getByRole("button", { name: "配置表单规则" }));
    fireEvent.click(view.getByRole("button", { name: "确定" }));

    const current = designerRef.current
      ?.getDefinition()
      .nodeList.find((node) => node.nodeCode === approval.nodeCode);
    expect(current && getApproverRule(current)).toMatchObject({
      strategy: "FORM_RULE",
      selectionType: "RELATION",
      relationType: "FORM_RULE",
      config: { field: "ownerId" },
    });
    expect(view.queryByRole("dialog", { name: "人员选择" })).toBeNull();
  });

  test("shows strategy options only on their configured node types", () => {
    const initial = createInitialDefinition();
    initial.nodeList = initial.nodeList.map((node) =>
      node.nodeType === "1"
        ? setApproverRule(node, "USER", [
            { id: "a", type: "USER" },
            { id: "b", type: "USER" },
          ])
        : node,
    );
    const approval = initial.nodeList.find((node) => node.nodeType === "1")!;
    const definition = insertNodeAfter(initial, approval.nodeCode, "8");
    const carbonCopy = definition.nodeList.find(
      (node) => node.nodeType === "8",
    )!;
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        defaultValue={definition}
      />,
    );

    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    expect(view.getByRole("combobox", { name: "多人审批策略" })).toBeTruthy();
    expect(
      view.getByRole("radiogroup", { name: "审批人与提交人为同一人时" }),
    ).toBeTruthy();
    const closeButtons = view.getAllByRole("button", { name: "关闭节点设置" });
    fireEvent.click(closeButtons[closeButtons.length - 1]);
    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${carbonCopy.nodeName}` }),
    );
    expect(view.queryByRole("combobox", { name: "多人审批策略" })).toBeNull();
    expect(
      view.queryByRole("radiogroup", { name: "审批人与提交人为同一人时" }),
    ).toBeNull();
  });

  test("hides multiple and empty policies for one concrete person", () => {
    const definition = createInitialDefinition();
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const policyOptions = [
      {
        code: "approvalMode",
        name: "多人审批策略",
        condition: "MULTIPLE" as const,
        choices: [{ value: "OR", label: "任意一人通过" }],
      },
      {
        code: "emptyPolicy",
        name: "无人审批策略",
        condition: "EMPTY" as const,
        choices: [{ value: "FAIL", label: "阻止提交" }],
      },
    ];
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        defaultValue={definition}
        capabilities={{
          schemaVersion: 1,
          nodeTypes: ["0", "1", "2"],
          approverStrategies: [
            {
              code: "USER",
              name: "指定人员",
              selectionType: "RESOURCE",
              resourceType: "USER",
              multiple: false,
              editorType: "DIALOG",
              resultCardinality: "EXACTLY_ONE",
              options: policyOptions,
            },
          ],
          approvalModes: ["OR"],
          returnPolicies: ["PREVIOUS"],
          timeoutNodeTypes: [],
          operations: ["SAVE"],
          resourceTypes: ["USER"],
        }}
      />,
    );

    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    fireEvent.change(view.getByLabelText("审批人"), {
      target: { value: "USER" },
    });
    expect(view.getByRole("button", { name: "选择指定人员" })).toBeTruthy();
    expect(view.queryByRole("combobox", { name: "多人审批策略" })).toBeNull();
    expect(view.queryByRole("radiogroup", { name: "无人审批策略" })).toBeNull();
  });

  test("delegates participant selection content to the host application", () => {
    const definition = createInitialDefinition();
    const approval = definition.nodeList.find((node) => node.nodeType === "1")!;
    const designerRef = createRef<ReactFlowDesignerRef>();
    const view = render(
      <ReactFlowDesigner
        ref={designerRef}
        defaultValue={definition}
        capabilities={DEMO_CAPABILITIES}
        renderApproverEditor={({ onChange }) => (
          <button
            type="button"
            onClick={() =>
              onChange([{ id: "u-100", type: "USER", name: "组织架构用户" }])
            }
          >
            从组织架构选择
          </button>
        )}
      />,
    );

    fireEvent.click(
      view.getByRole("button", { name: `编辑节点：${approval.nodeName}` }),
    );
    expect(view.queryByText("搜索用户")).toBeNull();
    fireEvent.change(view.getByLabelText("审批人"), {
      target: { value: "USER" },
    });
    fireEvent.click(view.getByRole("button", { name: "选择用户" }));
    expect(view.getByRole("dialog", { name: "人员选择" })).toBeTruthy();
    fireEvent.click(view.getByRole("button", { name: "从组织架构选择" }));
    fireEvent.click(
      within(view.getByRole("dialog", { name: "人员选择" })).getByRole(
        "button",
        { name: "确定" },
      ),
    );
    fireEvent.click(
      within(view.getByRole("dialog", { name: "节点设置" })).getByRole(
        "button",
        { name: "确定" },
      ),
    );

    const current = designerRef.current
      ?.getDefinition()
      .nodeList.find((node) => node.nodeCode === approval.nodeCode);
    expect(current && getApproverRule(current).subjects).toEqual([
      { id: "u-100", type: "USER", name: "组织架构用户" },
    ]);
    expect(view.getAllByText("组织架构用户")).toHaveLength(2);
  });

  test("renders branches into one directional merge path", () => {
    const initial = createInitialDefinition();
    const start = initial.nodeList.find((node) => node.nodeType === "0")!;
    const definition = insertNodeAfter(initial, start.nodeCode, "3");
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        defaultValue={definition}
      />,
    );

    expect(view.container.querySelectorAll(".frd-branch__name")).toHaveLength(
      0,
    );
    expect(
      view.container.querySelectorAll(".frd-branch__merge-tail"),
    ).toHaveLength(2);
    expect(view.container.querySelectorAll(".frd-merge-flow")).toHaveLength(1);
    expect(
      view.container.querySelectorAll(".frd-connector--fork"),
    ).toHaveLength(1);
    expect(
      view.container.querySelectorAll(".flovira-react-branch-grid"),
    ).toHaveLength(1);
    expect(view.queryByText(/汇合至/)).toBeNull();
    expect(
      view.getAllByRole("button", { name: "编辑节点：审批节点" }),
    ).toHaveLength(1);
  });

  test.each(["1", "3", "4", "5"] as const)(
    "inserts type %s inside a branch without duplicating its continuation",
    (type) => {
      const initial = createInitialDefinition();
      const start = initial.nodeList.find((node) => node.nodeType === "0")!;
      const definition = insertNodeAfter(initial, start.nodeCode, "3");
      const branch = definition.nodeList.find(
        (node) => node.nodeName === "分支一",
      )!;
      const sibling = definition.nodeList.find(
        (node) => node.nodeName === "分支二",
      )!;
      const continuation = initial.nodeList.find(
        (node) => node.nodeType === "1",
      )!;
      continuation.nodeName = "共同审批";
      definition.nodeList.find(
        (node) => node.nodeCode === continuation.nodeCode,
      )!.nodeName = continuation.nodeName;
      const ref = createRef<ReactFlowDesignerRef>();
      const view = render(
        <ReactFlowDesigner
          {...{ capabilities: DEMO_CAPABILITIES }}
          ref={ref}
          defaultValue={definition}
        />,
      );
      expect(view.container.querySelector(".frd-node__type")).toBeNull();
      fireEvent.click(
        view.getByRole("button", { name: "在 分支一 后添加节点" }),
      );
      const labels = {
        "1": "审批节点",
        "3": "条件分支",
        "4": "并行分支",
        "5": "多选分支",
      };
      fireEvent.click(
        view.getByRole("menuitem", { name: `添加${labels[type]}` }),
      );
      const updated = ref.current!.getDefinition();
      const insertedCode = updated.nodeList.find(
        (node) => node.nodeCode === branch.nodeCode,
      )!.skipList[0].targetNodeCode;
      expect(
        updated.nodeList.find((node) => node.nodeCode === insertedCode)!
          .nodeType,
      ).toBe(type);
      expect(
        updated.nodeList.find((node) => node.nodeCode === sibling.nodeCode)!
          .skipList,
      ).toEqual(sibling.skipList);
      expect(
        view.getAllByRole("button", { name: "编辑节点：共同审批" }),
      ).toHaveLength(1);
      expect(
        view.getAllByRole("button", { name: "编辑节点：结束" }),
      ).toHaveLength(1);
      expect(
        view.container.querySelectorAll(".frd-branch .frd-insert-point").length,
      ).toBeGreaterThanOrEqual(2);
      act(() => ref.current!.undo());
      expect(ref.current!.getDefinition().nodeList).toHaveLength(
        definition.nodeList.length,
      );
      act(() => ref.current!.redo());
      expect(ref.current!.getDefinition().nodeList).toHaveLength(
        updated.nodeList.length,
      );
    },
  );

  test("shows named and colored node options in the insert dropdown", () => {
    const view = render(
      <ReactFlowDesigner
        {...{ capabilities: DEMO_CAPABILITIES }}
        defaultValue={createInitialDefinition()}
      />,
    );

    fireEvent.click(view.getAllByRole("button", { name: /后添加节点/ })[0]);
    const approvalItem = view.getByRole("menuitem", { name: "添加审批节点" });
    expect(
      approvalItem
        .querySelector(".frd-dropdown-menu__icon")
        ?.getAttribute("style"),
    ).toContain("color");
    expect(view.getByRole("menuitem", { name: "添加条件分支" })).toBeTruthy();
    expect(view.getByRole("menuitem", { name: "添加等待节点" })).toBeTruthy();

    fireEvent.click(approvalItem);
    expect(
      view.getAllByRole("button", { name: "编辑节点：审批节点" }),
    ).toHaveLength(2);
    expect(view.queryByRole("menu")).toBeNull();
  });
});
