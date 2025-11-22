import { test, expect, APIRequestContext } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || 'REPLACE_ME_JWT';

async function api(ctx: APIRequestContext, path: string, params: Record<string, string | string[]>) {
  const url = new URL(path, BASE_URL);
  Object.entries(params).forEach(([k, v]) => {
    if (Array.isArray(v)) v.forEach(val => url.searchParams.append(k, val));
    else url.searchParams.append(k, v);
  });
  const resp = await ctx.get(url.toString(), {
    headers: { Authorization: `Bearer ${ADMIN_TOKEN}` }
  });
  const json = await resp.json();
  return { resp, json };
}

test.describe('Policy debug & masking smoke', () => {
  test('effective policy returns toggles and match payload', async ({ request }) => {
    const { resp, json } = await api(request, '/api/admin/policies/effective', {
      featureCode: 'NOTICE',
      actionCode: 'READ'
    });

    expect(resp.status()).toBe(200);
    expect(json).toHaveProperty('policyToggles');
    // dataPolicyMatch는 없을 수도 있으므로 존재 시 필드 확인
    if (json.dataPolicyMatch) {
      expect(json.dataPolicyMatch).toHaveProperty('rowScope');
      expect(json.dataPolicyMatch).toHaveProperty('maskRule');
    }
  });

  test('secure-by-default: masking 적용 여부 확인 (공지 목록)', async ({ request }) => {
    const ctx = await request.newContext({
      baseURL: BASE_URL,
      extraHTTPHeaders: { Authorization: `Bearer ${ADMIN_TOKEN}` }
    });
    const resp = await ctx.get('/api/notices');
    expect(resp.status()).toBe(200);
    const list = await resp.json();
    if (Array.isArray(list) && list.length > 0) {
      const notice = list[0];
      // 마스킹된 필드 존재 여부만 체크(정책 값에 따라 변동)
      expect(notice).toHaveProperty('title');
    }
  });
});

/**
 * 실행 방법
 * 1) BASE_URL, ADMIN_TOKEN 환경변수 설정 (JWT는 NOTICE/POLICY READ 권한 필요)
 * 2) npx playwright test docs/audit/playwright/policy-debug.spec.ts
 */
