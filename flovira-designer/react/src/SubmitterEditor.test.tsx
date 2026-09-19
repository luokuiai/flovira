// @vitest-environment jsdom
import { createRef } from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { ReactFlowDesigner } from "./ReactFlowDesigner";
import {
  createInitialDefinition,
  getSubmitterRule,
  setSubmitterRule,
  validateDefinition,
} from "./model";
import { DEMO_CAPABILITIES } from "../../examples/capabilities";
import type { ApproverSelector, ReactFlowDesignerRef } from "./types";

afterEach(cleanup);

test("start and end have no tabs or form permissions, start defaults to all", () => {
  const view = render(<ReactFlowDesigner capabilities={DEMO_CAPABILITIES} />);
  fireEvent.click(view.getByRole("button", { name: "编辑节点：开始" }));
  expect(view.queryByRole("tab")).toBeNull();
  expect(view.queryByRole("tabpanel")).toBeNull();
  expect((view.getByLabelText("可提交人员") as HTMLSelectElement).value).toBe(
    "ALL",
  );
  expect(view.getByRole("option", { name: "指定人员" })).toBeTruthy();
  expect(view.getByRole("option", { name: "指定角色" })).toBeTruthy();
  expect(view.queryByLabelText("审批人")).toBeNull();
  expect(view.queryByRole("table", { name: "节点表单权限" })).toBeNull();
  fireEvent.click(view.getByRole("button", { name: "取消" }));
  fireEvent.click(view.getByRole("button", { name: "编辑节点：结束" }));
  expect(view.queryByRole("tab")).toBeNull();
  expect(view.queryByLabelText("可提交人员")).toBeNull();
  expect(view.getByLabelText("节点名称")).toBeTruthy();
});

test.each(["USER", "ROLE"])(
  "submission %s uses host picker, preserves drafts and supports undo",
  async (strategy) => {
    const ref = createRef<ReactFlowDesignerRef>();
    const subject = { id: "allowed", type: strategy, name: "允许提交" };
    const selector = vi
      .fn<ApproverSelector>()
      .mockResolvedValue({ subjects: [subject] });
    const view = render(
      <ReactFlowDesigner
        ref={ref}
        capabilities={DEMO_CAPABILITIES}
        onSelectApprover={selector}
      />,
    );
    const rule = () =>
      getSubmitterRule(ref.current!.getDefinition().nodeList[0]);
    fireEvent.click(view.getByRole("button", { name: "编辑节点：开始" }));
    fireEvent.change(view.getByLabelText("可提交人员"), {
      target: { value: strategy },
    });
    expect(view.queryByText("审批人为空时")).toBeNull();
    fireEvent.click(
      view.getByRole("button", {
        name: strategy === "USER" ? "选择指定人员" : "选择指定角色",
      }),
    );
    await view.findByText("允许提交");
    expect(selector.mock.calls[0][0].node.nodeType).toBe("0");
    expect(rule().strategy).toBe("ALL");
    fireEvent.click(view.getByRole("button", { name: "确定" }));
    expect(rule().subjects).toEqual([subject]);
    fireEvent.click(view.getByRole("button", { name: "编辑节点：开始" }));
    expect(within(view.getByRole("dialog")).getByText("允许提交")).toBeTruthy();
    fireEvent.change(view.getByLabelText("可提交人员"), {
      target: { value: "ALL" },
    });
    fireEvent.click(view.getByRole("button", { name: "取消" }));
    expect(rule().strategy).toBe(strategy);
    act(() => ref.current!.undo());
    expect(rule().strategy).toBe("ALL");
  },
);

test("custom submission strategy uses the existing host picker and honors read-only state", async () => {
  const capabilities = {
    ...DEMO_CAPABILITIES,
    submitterStrategies: [
      {
        code: "PROJECT",
        version: 3,
        name: "项目成员",
        selectionType: "RESOURCE" as const,
        resourceType: "PROJECT",
        multiple: true,
        editorType: "DIALOG" as const,
      },
    ],
  };
  const selector = vi
    .fn<ApproverSelector>()
    .mockResolvedValue({ subjects: [{ id: "p1", type: "PROJECT" }] });
  const ref = createRef<ReactFlowDesignerRef>();
  const view = render(
    <ReactFlowDesigner
      ref={ref}
      capabilities={capabilities}
      onSelectApprover={selector}
    />,
  );
  fireEvent.click(view.getByRole("button", { name: "编辑节点：开始" }));
  fireEvent.change(view.getByLabelText("可提交人员"), {
    target: { value: "PROJECT" },
  });
  fireEvent.click(view.getByRole("button", { name: "选择项目成员" }));
  await waitFor(() => expect(view.getByText("p1")).toBeTruthy());
  fireEvent.click(view.getByRole("button", { name: "确定" }));
  expect(
    getSubmitterRule(ref.current!.getDefinition().nodeList[0]).strategyVersion,
  ).toBe(3);
  const saved = ref.current!.getDefinition();
  view.unmount();
  const readOnly = render(
    <ReactFlowDesigner
      defaultValue={saved}
      capabilities={capabilities}
      disabled
    />,
  );
  fireEvent.click(readOnly.getByRole("button", { name: "编辑节点：开始" }));
  expect(readOnly.getByLabelText("可提交人员").hasAttribute("disabled")).toBe(
    true,
  );
  expect(
    readOnly
      .getByRole("button", { name: "选择项目成员" })
      .hasAttribute("disabled"),
  ).toBe(true);
});

test("rejects malformed, unsupported and empty restricted rules instead of defaulting to all", () => {
  const definition = createInitialDefinition();
  const validate = () =>
    validateDefinition(definition, DEMO_CAPABILITIES).issues.filter(
      (issue) => issue.code === "SUBMITTER_INVALID",
    );
  expect(validate()).toEqual([]);
  definition.nodeList[0] = setSubmitterRule(definition.nodeList[0], "USER");
  expect(validate()).toHaveLength(1);
  definition.nodeList[0].ext = JSON.stringify({ submitterRule: "broken" });
  expect(validate()).toHaveLength(1);
  definition.nodeList[0] = setSubmitterRule(definition.nodeList[0], "UNKNOWN");
  expect(validate()).toHaveLength(1);
});
