/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.rule;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.http.HttpMethod;
import it.Category6Suite;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sonarqube.ws.Organizations;
import util.ItUtils;
import util.OrganizationRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newWsClient;

public class RuleTagsTest {

  private static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;
  private static OrganizationRule organizations = new OrganizationRule(orchestrator);

  @ClassRule
  public static TestRule chain = RuleChain.outerRule(orchestrator)
    .around(organizations);

  private static Organizations.Organization organization1;
  private static Organizations.Organization organization2;

  @BeforeClass
  public static void setUp() {
    organization1 = organizations.create();
    organization2 = organizations.create();
  }

  @Test
  public void should_not_show_tags_of_other_organization() {
    updateTag("foo-tag", organization1);
    updateTag("bar-tag", organization2);
    assertThat(showRuleTags(organization1)).containsExactly("foo-tag");
    assertThat(showRuleTags(organization2)).containsExactly("bar-tag");
  }

  @Test
  public void should_not_list_tags_of_other_organization() {
    updateTag("foo-tag", organization1);
    updateTag("bar-tag", organization2);
    assertThat(listTags(organization1))
      .contains("foo-tag")
      .doesNotContain("bar-tag");
  }

  @Test
  public void should_not_show_removed_tags() {
    updateTag("foo-tag", organization1);
    assertThat(showRuleTags(organization1)).contains("foo-tag");

    updateTag("", organization1);
    assertThat(showRuleTags(organization1)).isEmpty();
  }

  @Test
  public void should_not_list_removed_tags() {
    updateTag("foo-tag", organization1);
    assertThat(listTags(organization1)).contains("foo-tag");

    updateTag("", organization1);
    assertThat(listTags(organization1)).doesNotContain("foo-tag");
  }

  private List<String> listTags(Organizations.Organization organization) {
    String json = orchestrator.getServer().newHttpCall("/api/rules/tags")
      .setParam("organization", organization.getKey())
      .execute()
      .getBodyAsString();
    return (List<String>) ItUtils.jsonToMap(json).get("tags");
  }

  private List<String> showRuleTags(Organizations.Organization organization) {
    return newWsClient(orchestrator).rules().show(organization.getKey(), "xoo:OneIssuePerFile")
      .getRule().getTags().getTagsList();
  }

  private void updateTag(String tag, Organizations.Organization organization) {
    orchestrator.getServer().newHttpCall("/api/rules/update")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .setParam("organization", organization.getKey())
      .setParam("key", "xoo:OneIssuePerFile")
      .setParam("tags", tag)
      .execute();
  }
}