package org.wikidata.wdtk.datamodel.json.jackson;

/*
 * #%L
 * Wikidata Toolkit Data Model
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.Equality;
import org.wikidata.wdtk.datamodel.helpers.Hash;
import org.wikidata.wdtk.datamodel.helpers.ToString;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Jackson implementation of {@link ItemDocument}. Like all Jackson objects, it
 * is not technically immutable, but it is strongly recommended to treat it as
 * such in all contexts: the setters are for Jackson; never call them in your
 * code.
 *
 * @author Fredo Erxleben
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JacksonItemDocument extends JacksonTermedStatementDocument
		implements ItemDocument {

	/**
	 * Map to store site links.
	 */
	@JsonDeserialize(contentAs=JacksonSiteLink.class)
	private final Map<String, SiteLink> sitelinks;
	
	/**
	 * Constructor for instances that are built manually, rather than from JSON.
	 * 
	 * @param id
	 * @param labels
	 * @param descriptions
	 * @param aliases
	 * @param statements
	 * @param datatype
	 * @param revisionId
	 */
	public JacksonItemDocument(
			ItemIdValue id,
			List<MonolingualTextValue> labels,
			List<MonolingualTextValue> descriptions,
			List<MonolingualTextValue> aliases,
			List<StatementGroup> statements,
			List<SiteLink> siteLinks,
			long revisionId) {
		super(id, labels, descriptions, aliases, statements, revisionId);
		this.sitelinks = new HashMap<>();
		for(SiteLink sitelink : siteLinks) {
			if(this.sitelinks.containsKey(sitelink.getSiteKey())) {
				throw new IllegalArgumentException("Multiple site links provided for the same site.");
			} else {
				this.sitelinks.put(sitelink.getSiteKey(), sitelink);
			}
		}
	}

	/**
	 * Constructor. Creates an object that can be populated during JSON
	 * deserialization. Should only be used by Jackson for this very purpose.
	 */
	@JsonCreator
	public JacksonItemDocument(
			@JsonProperty("id") String jsonId,
			@JsonProperty("labels") Map<String, MonolingualTextValue> labels,
			@JsonProperty("descriptions") Map<String, MonolingualTextValue> descriptions,
			@JsonProperty("aliases") Map<String, List<MonolingualTextValue>> aliases,
			@JsonProperty("claims") Map<String, List<JacksonPreStatement>> claims,
			@JsonProperty("sitelinks") Map<String, SiteLink> sitelinks,
			@JsonProperty("lastrevid") long revisionId,
			@JacksonInject("siteIri") String siteIri) {
		super(jsonId, labels, descriptions, aliases, claims, revisionId, siteIri);
		if (sitelinks != null) {
			this.sitelinks = sitelinks;
		} else {
			this.sitelinks = Collections.<String, SiteLink>emptyMap();
		}
	}

	@JsonIgnore
	@Override
	public ItemIdValue getItemId() {
		if (this.siteIri == null) {
			return Datamodel.makeWikidataItemIdValue(this.entityId);
		} else {
			return Datamodel.makeItemIdValue(this.entityId, this.siteIri);
		}
	}

	@JsonIgnore
	@Override
	public EntityIdValue getEntityId() {
		return getItemId();
	}

	@JsonProperty("sitelinks")
	@Override
	public Map<String, SiteLink> getSiteLinks() {
		return Collections.<String, SiteLink> unmodifiableMap(this.sitelinks);
	}

	@Override
	public int hashCode() {
		return Hash.hashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		return Equality.equalsItemDocument(this, obj);
	}

	@Override
	public String toString() {
		return ToString.toString(this);
	}
}
