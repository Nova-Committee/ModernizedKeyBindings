package cn.novacommittee.mkb.api;

public interface IKeyConflictContext {
    boolean isActive();

    /** Either context can declare an overlap. */
    boolean conflicts(IKeyConflictContext other);
}
