package org.aksw.jena_sparql_api.data_client.algebra;

public class OpMapper
	implements OpVisitor<Op>
{
	@Override
	public Op visit(OpModel op) {
		System.out.println("Op: " + op);
		return null;
	}

	@Override
	public Op visit(OpConstruct op) {
		System.out.println("Op: " + op);
		return null;
	}

	@Override
	public Op visit(OpUpdateRequest op) {
		System.out.println("Op: " + op);
		return null;
	}

	@Override
	public Op visit(OpUnion op) {
		System.out.println("Op: " + op);
		return null;
	}

	@Override
	public Op visit(OpPersist op) {
		System.out.println("Op: " + op);
		return null;
	}
}
